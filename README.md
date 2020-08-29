LC Pinyin Analysis for Elasticsearch
====================================

Lc Pinyin版本
-------------

LC version | ES version
-----------|-----------
master | 6.7.0 -> master
6.7.0.0 | 6.7.0
5.3.0.1 | 5.3.0
5.2.2.1 | 5.2.2
5.2.0.1 | 5.2.0
5.1.2.1 | 5.1.2
5.1.1.1 | 5.1.1
5.0.2.1 | 5.0.2
5.0.1.2 | 5.0.1
5.0.1.1 | 5.0.1
2.4.2.1 | 2.4.2
2.2.2.1 | 2.2.2
1.4.5.2 | 1.4.5
1.4.5.1 | 1.4.5

Lc Pinyin介绍
-------------
> `elasticsearch-analysis-lc-pinyin`是一款`elasticsearch`拼音分词插件，可以支持按照全拼、首字母，中文混合搜索。
例如我们在某宝搜索框中输入“jianpan” 可以搜索到关键字包含“键盘”的商品。不仅仅输入全拼，有时候我们输入首字母、拼音和首字母、中文和首字母的混合输入，比如：“键pan”、“j盘”、“jianp”、“jpan”、“jianp”、“jp”
等等，都应该匹配到键盘。通过elasticsearch-analysis-lc-pinyin这个插件就能做到类似的搜索

> * 此拼音插件主要用在`短文档`的搜索上，如文章的标题、作者，商品的品牌等，不建议用在`长文档`中

分析器 - Analyzer
------
> * `lc_index` : 该分词器用于索引数据时指定，将中文转换为全拼和首字，同时保留中文
> * `lc_search`: 该分词器用于拼音搜索时指定，按最小拼音分词个数拆分拼音，优先拆分全拼

------

分词器 - Tokenizer
------
> * `lc_index`：参数 mode: *full_pinyin*，*first_letter*，*chinese_char*
> * `lc_search`：参数 mode: *smart_pinyin*，*single_letter*

------

过滤器 - TokenFilter
------
> * `lc_full_pinyin`：将中文Token转全拼(支持多音字)
> * `lc_first_letter`：将中文Token转首字母(支持多音字)

------
## 过滤器使用示例
以下是使用ik分词结合拼音过滤器使用例子：
1. 创建一个索引，并定义分析器
```bash
PUT /index
{
  "settings": {
    "analysis": {
      "analyzer": {
        "ik_letter_smart": {
          "type": "custom",
          "tokenizer": "ik_max_word",
          "filter": [
            "lc_first_letter"
          ]
        },
        "ik_py_smart": {
          "type": "custom",
          "tokenizer": "ik_max_word",
          "filter": [
            "lc_full_pinyin"
          ]
        }
      }
    }
  }
}
```
2. 测试分词
```bash
curl -XGET 'http://192.168.0.109:9200/index/_analyze?analyzer=ik_py_smart&pretty&text=英雄难过美人关'

#返回词条：
{
  "tokens" : [
    {
      "token" : "yingxiongnanguomeirenguan",
      "start_offset" : 0,
      "end_offset" : 7,
      "type" : "CN_WORD",
      "position" : 0
    },
    {
      "token" : "yingxiongnanguo",
      "start_offset" : 0,
      "end_offset" : 4,
      "type" : "CN_WORD",
      "position" : 1
    },
    {
      "token" : "yingxiong",
      "start_offset" : 0,
      "end_offset" : 2,
      "type" : "CN_WORD",
      "position" : 2
    },
    {
      "token" : "nanguo",
      "start_offset" : 2,
      "end_offset" : 4,
      "type" : "CN_WORD",
      "position" : 3
    },
    {
      "token" : "meirenguan",
      "start_offset" : 4,
      "end_offset" : 7,
      "type" : "CN_WORD",
      "position" : 4
    },
    {
      "token" : "meiren",
      "start_offset" : 4,
      "end_offset" : 6,
      "type" : "CN_WORD",
      "position" : 5
    },
    {
      "token" : "guan",
      "start_offset" : 6,
      "end_offset" : 7,
      "type" : "CN_CHAR",
      "position" : 6
    }
  ]
}
```

## 分析器使用示例

1.创建一个索引`index`

```bash
curl -XPUT http://localhost:9200/index
```

2.创建类型`brand`的mapping

```bash
curl -XPOST http://localhost:9200/index/_mapping/brand -d'
{
  "brand": {
    "properties": {
      "name": {
        "type": "text",
        "analyzer": "lc_index",
        "search_analyzer": "lc_search",
        "term_vector": "with_positions_offsets"
      }
    }
  }
}'
```

3.索引一些互联网品牌

```bash
curl -XPOST http://localhost:9200/index/brand/1 -d'{"name":"百度"}'
curl -XPOST http://localhost:9200/index/brand/8 -d'{"name":"百度糯米"}'
curl -XPOST http://localhost:9200/index/brand/2 -d'{"name":"阿里巴巴"}'
curl -XPOST http://localhost:9200/index/brand/3 -d'{"name":"腾讯科技"}'
curl -XPOST http://localhost:9200/index/brand/4 -d'{"name":"网易游戏"}'
curl -XPOST http://localhost:9200/index/brand/9 -d'{"name":"大众点评"}'
curl -XPOST http://localhost:9200/index/brand/10 -d'{"name":"携程旅行网"}'
```

4.编写高亮查询DSL

此示例通过`lc_search`分词器配合`match_phrase`查询实现品牌的`全拼`搜索
搜索全拼关键字`baidu`，请求DSL如下：
```bash
curl -XPOST http://localhost:9200/index/brand/_search  -d'
{
    "query": {
        "match": {
          "name": {
            "query": "baidu",
            "analyzer": "lc_search",
            "type": "phrase"
          }
        }
    },
    "highlight" : {
        "pre_tags" : ["<tag1>"],
        "post_tags" : ["</tag1>"],
        "fields" : {
            "name" : {}
        }
    }
}'
```

匹配到`百度`、`百度糯米`两个品牌
tip：`百度`排在`百度糯米`的前面，因为name字段长度更短
查询结果：
```bash
{
    "took": 18,
    "timed_out": false,
    "_shards": {
        "total": 1,
        "successful": 1,
        "failed": 0
    },
    "hits": {
        "total": 2,
        "max_score": 2.5751648,
        "hits": [
            {
                "_index": "index",
                "_type": "brand",
                "_id": "1",
                "_score": 2.5751648,
                "_source": {
                    "name": "百度"
                },
                "highlight": {
                    "name": [
                        "<tag1>百度</tag1>"
                    ]
                }
            }
            ,
            {
                "_index": "index",
                "_type": "brand",
                "_id": "8",
                "_score": 2.0601318,
                "_source": {
                    "name": "百度糯米"
                },
                "highlight": {
                    "name": [
                        "<tag1>百度</tag1>糯米"
                    ]
                }
            }
        ]
    }
}
```

此示例通过`lc_search`分词器配合`match_phrase`查询实现品牌的`中文&全拼`搜索
搜索全拼关键字`xie程lu行wang`，请求DSL如下：
```bash
curl -XPOST http://localhost:9200/index/brand/_search  -d'
{
    "query": {
        "match": {
          "name": {
            "query": "xie程lu行",
            "analyzer": "lc_search",
            "type": "phrase"
          }
        }
    },
    "highlight" : {
        "pre_tags" : ["<tag1>"],
        "post_tags" : ["</tag1>"],
        "fields" : {
            "name" : {}
        }
    }
}'
```

匹配到`携程旅行网` 结果如下：
```bash
#匹配到`携程旅行网` 结果如下：
{
    "took": 4,
    "timed_out": false,
    "_shards": {
        "total": 1,
        "successful": 1,
        "failed": 0
    },
    "hits": {
        "total": 1,
        "max_score": 4.5665164,
        "hits": [
            {
                "_index": "index",
                "_type": "brand",
                "_id": "10",
                "_score": 4.5665164,
                "_source": {
                    "name": "携程旅行网"
                },
                "highlight": {
                    "name": [
                        "<tag1>携程旅行</tag1>网"
                    ]
                }
            }
        ]
    }
}

```

```bash
# 此示例通过`lc_search`分词器配合`match_phrase`查询实现品牌的`首字母`搜索
# 此示例中也可以通过`lc_first_letter`分词器搜索，结果和`lc_search`一样
#
# 这两个分词器的主要区别:
#     lc_first_letterl 会把所有输入的字母拆分成单字母用于首字母匹配
#     lc_search        会优先把输入的字母串拆分成全拼,并找到一个最优拆分结果
#
# 搜索全拼关键字`albb`，请求DSL如下：
curl -XPOST http://localhost:9200/index/brand/_search  -d'
{
    "query": {
        "match": {
          "name": {
            "query": "albb",
            "analyzer": "lc_search",
            "type": "phrase"
          }
        }
    },
    "highlight" : {
        "pre_tags" : ["<tag1>"],
        "post_tags" : ["</tag1>"],
        "fields" : {
            "name" : {}
        }
    }
}'
```

匹配到`阿里巴巴`，结果如下：
```bash
{
    "took": 4,
    "timed_out": false,
    "_shards": {
        "total": 1,
        "successful": 1,
        "failed": 0
    },
    "hits": {
        "total": 1,
        "max_score": 3.9560113,
        "hits": [
            {
                "_index": "index",
                "_type": "brand",
                "_id": "2",
                "_score": 3.9560113,
                "_source": {
                    "name": "阿里巴巴"
                },
                "highlight": {
                    "name": [
                        "<tag1>阿里巴巴</tag1>"
                    ]
                }
            }
        ]
    }
}
```

```java
//java api实现
//该查询会匹配`阿里巴巴`这条数据
QueryBuilder pinyinQueryBuilder =  QueryBuilders.matchPhraseQuery("name", "ali巴b").analyzer("lc_search");
        SearchRequestBuilder requestBuilder = client.prepareSearch("index").setTypes("brand");
        requestBuilder.setQuery(pinyinQueryBuilder)
                .setHighlighterPreTags("<tag1>")
                .setHighlighterPostTags("</tag1>")
                .addHighlightedField("name")
                .execute().actionGet();
                
                
//该查询会匹配`大众点评`这条数据
QueryBuilder pinyinQueryBuilder =  QueryBuilders.matchPhraseQuery("name", "dzdp").analyzer("lc_first_letter");
        SearchRequestBuilder requestBuilder = client.prepareSearch("index").setTypes("brand");
        requestBuilder.setQuery(pinyinQueryBuilder)
                .setHighlighterPreTags("<tag1>")
                .setHighlighterPostTags("</tag1>")
                .addHighlightedField("name")
                .execute().actionGet();
                
//该查询也会匹配`大众点评`这条数据
QueryBuilder pinyinQueryBuilder =  QueryBuilders.matchPhraseQuery("name", "dzdp").analyzer("lc_search");
        SearchRequestBuilder requestBuilder = client.prepareSearch("index").setTypes("brand");
        requestBuilder.setQuery(pinyinQueryBuilder)
                .setHighlighterPreTags("<tag1>")
                .setHighlighterPostTags("</tag1>")
                .addHighlightedField("name")
                .execute().actionGet();
```
作者:  [@陈楠][1]
Email: 465360798@qq.com

<完>

[1]: http://blog.csdn.net/chennanymy?viewmode=contents