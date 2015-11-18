package com.KittyWeather.weather;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

public class StreamTool {
	public static String readLine(PushbackInputStream in) throws IOException {
		char buf[] = new char[128];
		int room = buf.length;
		int offset = 0;
		int c;
		loop: while (true) {
			switch (c = in.read()) {
			case -1:
			case '\n':
				break loop;
			case '\r':
				int c2 = in.read();
				if ((c2 != '\n') && (c2 != -1))
					in.unread(c2);
				break loop;
			default:
				if (--room < 0) {
					char[] lineBuffer = buf;
					buf = new char[offset + 128];
					room = buf.length - offset - 1;
					System.arraycopy(lineBuffer, 0, buf, 0, offset);

				}
				buf[offset++] = (char) c;
				break;
			}
		}
		if ((c == -1) && (offset == 0))
			return null;
		return String.copyValueOf(buf, 0, offset);
	}

	public static byte[] read(InputStream input) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] data = new byte[1024];
		int len = 0;
		while ((len = input.read(data)) != -1) {
			out.write(data, 0, len);
		}
		out.close();
		return out.toByteArray();
	}
}
