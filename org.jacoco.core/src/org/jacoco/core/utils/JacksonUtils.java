/*******************************************************************************
 * Copyright (c) 2009, 2021 Mountainminds GmbH & Co. KG and Contributors
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Marc R. Hoffmann - initial API and implementation
 *
 *******************************************************************************/
package org.jacoco.core.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link JacksonUtils}
 * <p>
 *
 * @author zhaoyb1990
 */
public class JacksonUtils {

	private static ObjectMapper mapper = new ObjectMapper();

	static {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		mapper.setDateFormat(sdf);
	}

	public static String serialize(Object object)
			throws JsonProcessingException {
		return serialize(object, true);
	}

	public static String serialize(Object object, boolean pretty)
			throws JsonProcessingException {
		return pretty
				? mapper.writerWithDefaultPrettyPrinter()
						.writeValueAsString(object)
				: mapper.writeValueAsString(object);
	}

	public static byte[] serialize2Bytes(Object object)
			throws JsonProcessingException {
		return mapper.writeValueAsBytes(object);
	}

	public static <T> T deserialize(byte[] bytes, Class<T> type)
			throws IOException {
		return mapper.readValue(bytes, type);
	}

	public static <T> T deserialize(String sequence, Class<T> type)
			throws JsonProcessingException {
		return mapper.readValue(sequence, type);
	}

	public static <T> List<T> deserializeArray(byte[] bytes, Class<T> type)
			throws IOException {
		return mapper.readValue(bytes,
				getCollectionType(ArrayList.class, type));
	}

	public static <T> List<T> deserializeArray(String sequence, Class<T> type)
			throws JsonProcessingException {
		return mapper.readValue(sequence,
				getCollectionType(ArrayList.class, type));
	}

	public static Object deserialize(byte[] bytes) throws IOException {
		return mapper.readTree(bytes);
	}

	private static JavaType getCollectionType(Class<?> collectionClass,
			Class<?>... elementClasses) {
		return mapper.getTypeFactory().constructParametrizedType(
				collectionClass, collectionClass, elementClasses);
	}
}
