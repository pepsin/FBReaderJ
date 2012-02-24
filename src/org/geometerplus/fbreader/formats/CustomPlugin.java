/*
 * Copyright (C) 2007-2012 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.fbreader.formats;

import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.library.Book;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.image.ZLImage;
import org.geometerplus.zlibrary.core.options.ZLStringOption;

public class CustomPlugin extends FormatPlugin {
	private static class DefaultInfoReader implements InfoReader {
		public boolean readMetaInfo(Book book) {
			return true;
		}
		public ZLImage readCover(ZLFile file) {
			return null;
		}
		public String readAnnotation(ZLFile file) {
			return null;
		}
	}

	private InfoReader myInfoReader;

	CustomPlugin(String fileType) {
		super(fileType);
		myInfoReader = new DefaultInfoReader();
	}

	CustomPlugin(String fileType, InfoReader ir) {
		super(fileType);
		myInfoReader = ir;
	}

	public String getPackage() {
		return Formats.extensionOption(supportedFileType()).getValue();
	}

	@Override
	public Type type() {
		return Type.EXTERNAL;
	}

	@Override
	public boolean readMetaInfo(Book book) {
		return myInfoReader.readMetaInfo(book);
	}

	@Override
	public boolean readLanguageAndEncoding(Book book) {
		return true;
	}

	@Override
	public boolean readModel(BookModel model) {
		return false;
	}

	@Override
	public ZLImage readCover(ZLFile file) {
		return myInfoReader.readCover(file);
	}

	@Override
	public String readAnnotation(ZLFile file) {
		return myInfoReader.readAnnotation(file);
	}
}
