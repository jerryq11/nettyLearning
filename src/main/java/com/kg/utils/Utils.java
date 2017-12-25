package com.kg.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Utils {

	/**
	 * 判断对象是否为空<br />
	 * 不为空返回true，为空返回false
	 * 
	 * @param o
	 * @return
	 */
	public static boolean notEmpty(Object o) {
		boolean notEmpty = false;
		if (o instanceof String) {
			String s = (String) o;
			if (s != null && !"".equals(s) && !"undefined".equals(s)
					&& !"null".equals(s)) {
				notEmpty = true;
			}
		} else if (o instanceof Collection) {
			Collection c = (Collection) o;
			if (c != null && c.size() > 0) {
				notEmpty = true;
			}
		} else if (o instanceof Object[]) {
			Object[] arr = (Object[]) o;
			if (arr != null && arr.length > 0) {
				notEmpty = true;
			}
		} else if (o != null) {
			return true;
		}
		return notEmpty;
	}
    
}
