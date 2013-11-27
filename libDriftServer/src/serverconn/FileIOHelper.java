package serverconn;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * ��������ļ���д
 * @author BorisHe
 *
 */
public class FileIOHelper {
	
	/**
	 * д�뵽�ļ�
	 * @param filename Ŀ��Ŀ¼
	 * @param bytes
	 * @return д���File�������Ϊʧ�ܷ���null
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
