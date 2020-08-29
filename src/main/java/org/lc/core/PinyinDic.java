package org.lc.core;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.Loggers;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

public class PinyinDic {

    private static final Logger logger = Loggers.getLogger(PinyinDic.class,PinyinDic.class.getName());

    public static final String dicLocation = "/pinyin.dic";

    public Set<String> dicSet = new HashSet<String>();

    private static PinyinDic instance;

    private PinyinDic() {
        initialize();
    }

    private void initialize() {
        InputStream in = PinyinDic.class.getResourceAsStream(dicLocation);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        try {
            String line = null;
            long startPoint = System.currentTimeMillis();
            while (null != (line = reader.readLine())) {
                if (line.trim().length() > 0) {
                    dicSet.add(line);
                }
            }
            long endPoint = System.currentTimeMillis();
            logger.info(String.format("Load pinyin from pinyin.dic, takes %s ms, size=%s", (endPoint - startPoint), dicSet.size()), this);
        } catch (Exception ex) {
            logger.error("read pinyin dic error.", ex);
            throw new RuntimeException("read pinyin dic error.", ex);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception ex) {
                //ignore ex
            }
        }
    }

    public static PinyinDic getInstance() {
        if (instance == null) {
            synchronized (PinyinDic.class) {
                if (instance == null) {
                    instance = new PinyinDic();
                }
            }
        }
        return instance;
    }

    public boolean contains(String c) {
        return dicSet.contains(c);
    }
}
