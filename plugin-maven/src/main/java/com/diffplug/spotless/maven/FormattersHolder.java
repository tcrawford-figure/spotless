/*
 * Copyright 2021 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.spotless.maven;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;

import com.diffplug.spotless.Formatter;

class FormattersHolder implements AutoCloseable {

	private final Map<Formatter, Supplier<Iterable<File>>> formatterToFiles;

	FormattersHolder(Map<Formatter, Supplier<Iterable<File>>> formatterToFiles) {
		this.formatterToFiles = formatterToFiles;
	}

	static FormattersHolder create(Map<FormatterFactory, Supplier<Iterable<File>>> formatterFactoryToFiles, FormatterConfig config) {
		Map<Formatter, Supplier<Iterable<File>>> formatterToFiles = new HashMap<>();
		try {
			for (Entry<FormatterFactory, Supplier<Iterable<File>>> entry : formatterFactoryToFiles.entrySet()) {
				FormatterFactory formatterFactory = entry.getKey();
				Supplier<Iterable<File>> files = entry.getValue();

				Formatter formatter = formatterFactory.newFormatter(files, config);
				formatterToFiles.put(formatter, files);
			}
		} catch (RuntimeException openError) {
			try {
				close(formatterToFiles.keySet());
			} catch (Exception closeError) {
				openError.addSuppressed(closeError);
			}
			throw openError;
		}

		return new FormattersHolder(formatterToFiles);
	}

	Iterable<Formatter> getFormatters() {
		return formatterToFiles.keySet();
	}

	Map<Formatter, Supplier<Iterable<File>>> getFormattersWithFiles() {
		return formatterToFiles;
	}

	@Override
	public void close() {
		try {
			close(formatterToFiles.keySet());
		} catch (Exception e) {
			throw new RuntimeException("Unable to close formatters", e);
		}
	}

	private static void close(Set<Formatter> formatters) throws Exception {
		Exception error = null;
		for (Formatter formatter : formatters) {
			try {
				formatter.close();
			} catch (Exception e) {
				if (error == null) {
					error = e;
				} else {
					error.addSuppressed(e);
				}
			}
		}
		if (error != null) {
			throw error;
		}
	}
}
