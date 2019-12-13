package webserver;

import java.io.BufferedReader;
import java.io.FileReader;


public class ResourceReader {
	public static String readFile(String name) {
		BufferedReader br;
		String buf = "";
		String ret = "";
		try {
			br = new BufferedReader(new FileReader(name));
			while((buf = br.readLine()) != null) {
				ret += buf + "\n";
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}
}
