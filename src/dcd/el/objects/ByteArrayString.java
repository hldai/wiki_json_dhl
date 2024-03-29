// author: DHL brnpoem@gmail.com

package dcd.el.objects;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import org.apache.commons.lang3.text.translate.CharSequenceTranslator;

// string stored as a byte array
// used to save memory
public class ByteArrayString implements Comparable<ByteArrayString> {
	public ByteArrayString() {

	}

	public ByteArrayString(String s) {
		try {
			bytes = s.getBytes("UTF8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	public ByteArrayString(String s, int len) {
		bytes = new byte[len];
		try {
			byte[] tmpBytes = s.getBytes("UTF8");
			int idx = 0;
			while (idx < tmpBytes.length) {
				bytes[idx] = tmpBytes[idx];
				++idx;
			}
			
			while (idx < len)
				bytes[idx++] = 0;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	@Override
	public int compareTo(ByteArrayString basRight) {
		for (int i = 0; i < bytes.length && i < basRight.bytes.length; ++i) {
			if (bytes[i] < basRight.bytes[i]) {
				return -1;
			}

			if (bytes[i] > basRight.bytes[i]) {
				return 1;
			}
		}

		if (bytes.length == basRight.bytes.length)
			return 0;
		if (bytes.length < basRight.bytes.length) {
			for (int i = bytes.length; i < basRight.bytes.length; ++i) {
				if (basRight.bytes[i] != 0)
					return -1;
			}
			return 0;
		} else {
			for (int i = basRight.bytes.length; i < bytes.length; ++i) {
				if (bytes[i] != 0)
					return 1;
			}
			return 0;
		}
	}

	public void fromString(String s) {
		try {
			bytes = s.getBytes("UTF8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public String toString() {
//		return new String(bytes);
		try {
			return new String(bytes, "UTF8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
	
//	public void toUTF8() {
//		try {
//			String tmp = new String(bytes, "ASCII");
//			bytes = tmp.getBytes("UTF8");
//		} catch (UnsupportedEncodingException e) {
//			e.printStackTrace();
//		}
//	}

	public void toFileWithByteLen(DataOutputStream dos) {
		try {
			if (bytes.length > 127) {
				System.err.println("Length larger than 127!");
			}
			dos.write(bytes.length);
			dos.write(bytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void toFileWithShortByteLen(DataOutputStream dos) {
		try {
			dos.writeShort(bytes.length);
			dos.write(bytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void toFileWithFixedLen(DataOutputStream dos, int len) {
		try {
			dos.write(bytes);
			
			int cnt = len - bytes.length;
			while (cnt-- > 0)
				dos.write(0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void fromFileWithByteLen(DataInputStream dis) {
		try {
			byte len = dis.readByte();
			bytes = new byte[len];
			dis.read(bytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void fromFileWithShortByteLen(DataInputStream dis) {
		try {
			short len = dis.readShort();
			bytes = new byte[len];
			dis.read(bytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void fromFileWithFixedLen(DataInputStream dis, int len) {
		bytes = new byte[len];
		try {
			dis.read(bytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void fromFileWithFixedLen(RandomAccessFile raf, int len) {
		bytes = new byte[len];
		try {
			raf.read(bytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public byte[] bytes = null;
}
