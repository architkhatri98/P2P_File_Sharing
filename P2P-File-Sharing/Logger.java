import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Logger {
	static FileOutputStream file;
	static OutputStreamWriter output;

	public static void start(String f) throws IOException {
		file = new FileOutputStream(f);
		output = new OutputStreamWriter(file, "UTF-8");
	}

	public static void log_write(String str) {
		try {
			output.write(str + '\n');
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String set_time_format() {
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat sdformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return sdformat.format(calendar.getTime());
	}

	public static void stop() {
		try {
			output.flush();
			file.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
