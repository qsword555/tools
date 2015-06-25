package org.qipeng.tools;

import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;

import javax.servlet.http.*;

import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.*;
import org.apache.commons.fileupload.servlet.*;




/**
 * 本类为基于common-fileupload支持多文件上传的工具类
 * 使用方式：
 * 		1.先对本类进行初始化，本类只有一个构造方法，FileUploadTool fut = new FileUploadTool(request);
 * 		2.设置一些限制条件，当然也可以选择不限制，采用默认方式
 * 			 fut.setEncoding("gbk")            //设置编码，默认为utf-8
 *				.setSingleAllowSize(3*1024*1024)   //设置单文件最大允许上传大小(默认无限制)
 *				.setTotalAllowSize(10*1024*1024)   //设置允许上传的所有文件的大小(默认无限制)
 *				.setTempDir("D:\\tmp",512*1024)    //设置临时目录和大小
 *				.setNeedRename(true);           //设置是否需要重命名，默认为false
 *		3.设置完成后，必须执行init()方法.来进行一些初始化工作   fut.init();
 *		4.本类提供了两个限制文件类型上传的方法isAllowed(...)和isNotAllowed(...)，可以不判断
 *		5.执行saveDir(path)方法,保存
 * @author peng.qi
 *
 */
public class FileUploadTool {
	
	private String encoding = "UTF-8";   //编码格式
	private long singleAllowSize = 0;   //允许上传的单文件的最大大小   超过会报 
	private long totalAllowSize = 0;   //允许上传的总文件的最大大小   
	private String tempDir = null;  //文件上传的临时目录，大文件上传建议使用
	private boolean needRename = false;  //是否需要重命名,如果不需要的话，有重名文件默认覆盖
	private int maxMemorySize = 0;      //设定使用内存超过时，将产生临时文件并存储于临时目录中。     

	private HttpServletRequest request = null;               
	private List<FileItem> items = null;
	private Map<String, List<String>> params = new HashMap<String, List<String>>();
	private List<FileItem> fileitems = new ArrayList<FileItem>();

	
	public FileUploadTool(HttpServletRequest request){
		this.request = request;
	}
	
	@SuppressWarnings("unchecked")
	public FileUploadTool init(){
		DiskFileItemFactory factory = new DiskFileItemFactory();
		if(maxMemorySize>0 && !isNullOrEmpty(this.tempDir)){    
			factory.setSizeThreshold(maxMemorySize);
			factory.setRepository(new File(tempDir));
		}
		ServletFileUpload upload = new ServletFileUpload(factory);
		if(singleAllowSize>0){
			upload.setSizeMax(singleAllowSize);  //设置单个文件上传的最大大小
		}
		if(totalAllowSize>0){
			upload.setFileSizeMax(totalAllowSize);  //设置上传的所有文件的最大大小
		}
		upload.setHeaderEncoding(encoding);   
		try {
			this.items = upload.parseRequest(request);
		} catch (FileUploadException e) {
			e.printStackTrace();
		}
		parseRequest();
		return this;
	}
	
	
	/**
	 * 解析request对象，获得对象和普通字段
	 */
	private void parseRequest(){
		Iterator<FileItem> iter = this.items.iterator();

		while (iter.hasNext()) {
			FileItem item = iter.next();
			if (item.isFormField()) { // 是普通数据
				String name = item.getFieldName();
				String value = null;
				try {
					value = item.getString(encoding);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				List<String> temp = null;
				if (this.params.containsKey(name)) {
					temp = this.params.get(name);
				} else {
					temp = new ArrayList<String>();
				}
				temp.add(value);
				this.params.put(name, temp);
			} else { // 是文件类型
				this.fileitems.add(item);
			}
		}
	}
	
	/**
	 * 保存上传的所有文件
	 * @param saveDir 要保存的目录
	 * @return 返回保存后的每个文件名称
	 * @throws Exception
	 */
	public List<String> saveAll(String saveDir){
		List<String> names = new ArrayList<String>();
		if (this.fileitems.size() > 0) { // 有内容
			// 循环将要上传的文件 依次保存到指定的文件夹中
			for (FileItem item : fileitems) {
				String fileName = null;
				if (needRename) {
					fileName = getIPTimeRand(this.request.getRemoteAddr())
							+ getFileExtWithPoint(item.getName());
				} else {
					fileName = item.getName();
				}
				names.add(fileName);
				try {
					item.write(new File(saveDir, fileName));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return names; 
	}
	
	/**
	 * 判断是否是允许上传的文件类型(如果有一个文件不是，则返回false)
	 * @param ext sample:jpg,png 要求，后缀要全部小写
	 * @return true | false
	 */
	public boolean isAllowed(String ...exts){
		if (this.fileitems.size() > 0) { // 有内容
			for (FileItem item : fileitems) {
				String fileName = item.getName();
				if(inArray(getFileExt(fileName).toLowerCase(),exts)){
					continue;
				}
				return false;
			}
			return true;
		}
		return false;
	}
	
	/**
	 * 判断是否有不允许的文件类型的文件存在，如果存在则返回true,不存在返回false
	 * @param ext sample:ext,html 要求，后缀要全部小写
	 * @return true | false
	 */
	public boolean isNotAllowed(String ...exts){
		if (this.fileitems.size() > 0) { // 有内容
		for(FileItem item : fileitems){
			String fileName = item.getName();
			if(inArray(getFileExt(fileName).toLowerCase(),exts)){
				return true;
			}
		}
			return false;
		}
		return false;
	}
	
	/**
	 * 获得request中的参数
	 * @param name 参数名
	 * @return 参数的值
	 */
	public String getParameter(String name) {
		List<String> list = this.params.get(name);
		if (list != null) {
			return list.toArray(new String[] {})[0];
		}
		return null;
	}

	/**
	 * 获得request中的参数
	 * @param name 参数名
	 * @return 参数值数组
	 */
	public String[] getParameterValues(String name) {
		List<String> list = this.params.get(name);
		if (list != null) {
			return list.toArray(new String[] {});
		}
		return null;
	}

	/**
	 * 设置编码
	 * @param encoding 默认为utf-8
	 * @return
	 */
	public FileUploadTool setEncoding(String encoding) {
		this.encoding = encoding;
		return this;
	}

	/**
	 * 设置允许上传的单文件的最大大小(如果超过，会产生异常SizeLimitExceededException)
	 * @param singleAllowSize 默认无限制
	 * @return 
	 */
	public FileUploadTool setSingleAllowSize(long singleAllowSize) {
		this.singleAllowSize = singleAllowSize;
		return this;
	}

	/**
	 * 设置允许上传的所有文件的总大小(如果超过，会产生异常FileSizeLimitExceededException)
	 * @param totalAllowSize 默认无限制
	 * @return
	 */
	public FileUploadTool setTotalAllowSize(long totalAllowSize) {
		this.totalAllowSize = totalAllowSize;
		return this;
	}

	/**
	 * 设置临时目录，和大小（当上传文件超过了maxMemorySize的时候，则先上传到临时目录）
	 * @param tempDir
	 * @param maxMemorySize
	 * @return
	 */
	public FileUploadTool setTempDir(String tempDir,int maxMemorySize) {
		this.tempDir = tempDir;
		this.maxMemorySize = maxMemorySize;
		return this;
	}
	
	/**
	 * 设置是否需要重新命名文件(true:IP+TimeStamp+3位随机数    | false：采用原文件名)
	 * @param needRename 默认为false
	 * @return
	 */
	public FileUploadTool setNeedRename(boolean needRename) {
		this.needRename = needRename;
		return this;
	}
	

	/**
	 * 判断str是否是""或者null
	 * @param str
	 * @return ture | false
 	 */
	public boolean isNullOrEmpty(String str){
		if(str==null || "".equals(str)){
			return true;
		}
		return false;
	}
	
	/**
	 * 判断字符串是否在字符串数组中
	 * @param str 字符串
	 * @param arr 目标数组
	 * @return true | false 
	 */
	public static boolean inArray(String str,String[] arr){
		return Arrays.asList(arr).contains(str);
	}
	
	/**
	 * 获取文件后缀
	 * @param filename
	 * @return sample(.txt .jpg等);
	 */
	public static String getFileExtWithPoint(String filename){
		if(filename!=null && filename.indexOf(".")!=-1){
			String s[] = filename.split("\\.");
			return "."+s[s.length-1];
		}
		return "";
	}
	
	/**
	 * 获取文件后缀
	 * @param filename
	 * @return sample(txt jpg等);
	 */
	public static String getFileExt(String filename){
		if(filename!=null && filename.indexOf(".")!=-1){
			String s[] = filename.split("\\.");
			return s[s.length-1];
		}
		return "";
	}
	
	/**
	 * 用于构造一个唯一的字符串
	 * @param ip ip地址
	 * @return IP+TimeStamp+3位随机数
	 */
	public static String getIPTimeRand(String ip) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		StringBuffer buffer = new StringBuffer();
		if (ip.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) {
			String str[] = ip.split("\\.");
			for (int i = 0; i < str.length; i++) {
				StringBuffer buf = new StringBuffer();
				buf.append(str[i]);
				while (buf.length() < 3) {
					buf.insert(0, 0);
				}
				buffer.append(buf);
			}
			buffer.append(sdf.format(new Date()));
			Random ran = new Random();
			for (int i = 0; i < 3; i++) {
				buffer.append(ran.nextInt(10));
			}
		}
		return buffer.toString();
	}
	
}