package serverconn;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 负责管理文件读写
 * @author BorisHe
 *
 */
public class FileIOHelper {
	
	/**
	 * 写入到文件
	 * @param filename 目标目录
	 * @param bytes
	 * @return 写入的File对象，如果为失败返回null
	 */
	public static File writeToFile(String filename, byte[] bytes){
		try {
			File file = new File(filename);
			if(file.exists()){
				boolean b = file.delete();
				if(!b){
					throw new IOException();
				}
			}
			FileOutputStream outputStream = new FileOutputStream(file);
			outputStream.write(bytes);
			outputStream.close();
			
			return file;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
}
