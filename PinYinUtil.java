package org.qipeng.util;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

/**
 * 此类用于将汉字转换为汉语拼音的工具类
 * 
 * */
public class PinYinUtil {
	/**
	 * 将汉字转换为拼音,英文字符数字不变，支持多音字，所以返回值为一个String的数组，
	 * 里面是各种组合，例如"音乐"会返回"yinyue,yinle"
	 * 
	 * @param chinese
	 *            需要转换的中文字符串
	 * @param isDeleteSpecialCharacter
	 *            为true时会删除特殊字符，为false时不会
	 * */
	public static String[] getPolyphonePinYin(String chinese,
			Boolean isDeleteSpecialCharacter)
			throws BadHanyuPinyinOutputFormatCombination {
		if ("".equals(chinese) || chinese == null) {
			return null;
		}
		if (isDeleteSpecialCharacter == null) {
			isDeleteSpecialCharacter = false;
		}
		HanyuPinyinOutputFormat defaultFormat = new HanyuPinyinOutputFormat();
		defaultFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);// 设置小写
		defaultFormat.setVCharType(HanyuPinyinVCharType.WITH_V);// 设置用v代替ü
		defaultFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);// 设置不显示声调
		char[] c = chinese.toCharArray();
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < c.length; i++) {
			if (Character.toString(c[i]).matches("[\\u4E00-\\u9FA5]+")) { // 判断是否为汉字
				String temp[] = PinyinHelper.toHanyuPinyinStringArray(c[i], // 将一个汉字的所有读音以字符串数组的形式返回
						defaultFormat);
				for (int j = 0; j < temp.length; j++) {
					buffer.append(temp[j]);
					if (j != temp.length - 1) { // 此处为1个标记，用于简单标记一下这个汉字的各种读音之间的间隔
												// 例如乐: yue,le
						buffer.append(",");
					}
				}
			} else {
				if (isDeleteSpecialCharacter == true) {
					String temp = Character.toString(c[i]);
					if (!temp // 判断是否为特殊字符
							.matches("[`~!@#$^&*()=|{}':;',\\[\\].<>/?~！@#￥……&*（）——|{}【】‘；：”“'。，、？]")) {
						buffer.append(temp);
					}
				} else {
					buffer.append(c[i]);
				}
			}
			buffer.append(" "); // 此处同为一个标记，为了将各个汉字的拼音以空格为分隔符区分开来，方便后续调用
		}
		List<String> list = parseTheChineseByObject(discountTheChinese(buffer
				.toString()));

		return list.toArray(new String[list.size()]);
	}

	/**
	 * 去除多音字重复数据
	 * 
	 * @param theStr
	 * @return
	 */
	private static List<Map<String, Integer>> discountTheChinese(String theStr) {
		// 去除重复拼音后的拼音列表
		List<Map<String, Integer>> mapList = new ArrayList<Map<String, Integer>>();
		// 用于处理每个字的多音字，去掉重复
		Map<String, Integer> onlyOne = null;

		String[] firsts = theStr.split(" ");
		// 读出每个汉字的拼音
		for (String str : firsts) {
			onlyOne = new Hashtable<String, Integer>();
			String[] china = str.split(",");
			// 多音字处理
			for (String s : china) {
				Integer count = onlyOne.get(s);
				if (count == null) {
					onlyOne.put(s, new Integer(1));
				} else {
					onlyOne.remove(s);
					count++;
					onlyOne.put(s, count);
				}
			}
			mapList.add(onlyOne);
		}
		return mapList;
	}

	/**
	 * 解析并组合拼音，对象合并方案(推荐使用)
	 * 
	 * @return
	 */
	private static List<String> parseTheChineseByObject(
			List<Map<String, Integer>> list) {
		Map<String, Integer> first = null; // 用于统计每一次,集合组合数据
		// 遍历每一组集合
		for (int i = 0; i < list.size(); i++) {
			// 每一组集合与上一次组合的Map
			Map<String, Integer> temp = new Hashtable<String, Integer>();
			// 第一次循环，first为空
			if (first != null) {
				// 取出上次组合与此次集合的字符，并保存
				for (String s : first.keySet()) {
					for (String s1 : list.get(i).keySet()) {
						String str = s + s1;
						temp.put(str, 1);
					}
				}
				// 清理上一次组合数据
				if (temp != null && temp.size() > 0) {
					first.clear();
				}
			} else {
				for (String s : list.get(i).keySet()) {
					String str = s;
					temp.put(str, 1);
				}
			}
			// 保存组合数据以便下次循环使用
			if (temp != null && temp.size() > 0) {
				first = temp;
			}
		}
		List<String> result = new ArrayList<String>();
		if (first != null) {
			// 遍历取出组合字符串
			for (String str : first.keySet()) {
				result.add(str);
			}
		}
		return result;
	}

	/**
	 * 解析并组合拼音，循环读取方案（不灵活，不推荐使用）
	 * 
	 * @deprecated 现在有如下几个数组: {1,2,3} {4,5} {7,8,9} {5,2,8}
	 *             要求写出算法对以上数组进行数据组合,如:1475
	 *             ,1472,1478,1485,1482....如此类推，得到的组合刚好是以上数组的最隹组合（不多不少）.
	 *             注：要求有序组合
	 *             （并非象“全排列算法”那般得到的组合是无序的）：组合过程中，第一组数组排第一位、第二组排第二位、第三组排第三位....
	 * 
	 * @param list
	 * @return
	 */
	private static String parseTheChineseByFor(List<Map<String, Integer>> list) {
		StringBuffer sbf = new StringBuffer();
		int size = list.size();
		switch (size) {
		case 1:
			for (String s : list.get(0).keySet()) {
				String str = s;
				sbf.append(str + ",");
			}
			break;
		case 2:
			for (String s : list.get(0).keySet()) {
				for (String s1 : list.get(1).keySet()) {
					String str = s + s1;
					sbf.append(str + ",");
				}
			}
			break;
		case 3:
			for (String s : list.get(0).keySet()) {
				for (String s1 : list.get(1).keySet()) {
					for (String s2 : list.get(2).keySet()) {
						String str = s + s1 + s2;
						sbf.append(str + ",");
					}
				}
			}
			break;
		// 此处省略了数据组装过程，组装后的数据结构如下。
		// 注:List<Map<String, Integer>> list：List存的就是有多少组数据上面的是4组
		// Map就是具体的某一个数组（此处用Map主要是方便对其中数组中重复元素作处理）
		// StringBuffer sbf = new StringBuffer();--用于记录组合字符的缓冲器
		case 4:
			for (String s : list.get(0).keySet()) {
				for (String s1 : list.get(1).keySet()) {
					for (String s2 : list.get(2).keySet()) {
						for (String s3 : list.get(3).keySet()) {
							String str = s + s1 + s2 + s3;
							// 此处的sbf为StringBuffer
							sbf.append(str + ",");
						}
					}
				}
			}
			break;
		case 5:
			for (String s : list.get(0).keySet()) {
				for (String s1 : list.get(1).keySet()) {
					for (String s2 : list.get(2).keySet()) {
						for (String s3 : list.get(3).keySet()) {
							for (String s4 : list.get(4).keySet()) {
								String str = s + s1 + s2 + s3 + s4;
								sbf.append(str + ",");
							}
						}
					}
				}
			}
			break;
		case 6:
			for (String s : list.get(0).keySet()) {
				for (String s1 : list.get(1).keySet()) {
					for (String s2 : list.get(2).keySet()) {
						for (String s3 : list.get(3).keySet()) {
							for (String s4 : list.get(4).keySet()) {
								for (String s5 : list.get(5).keySet()) {
									String str = s + s1 + s2 + s3 + s4 + s5;
									sbf.append(str + ",");
								}
							}
						}
					}
				}
			}
			break;
		case 7:
			for (String s : list.get(0).keySet()) {
				for (String s1 : list.get(1).keySet()) {
					for (String s2 : list.get(2).keySet()) {
						for (String s3 : list.get(3).keySet()) {
							for (String s4 : list.get(4).keySet()) {
								for (String s5 : list.get(5).keySet()) {
									for (String s6 : list.get(6).keySet()) {
										String str = s + s1 + s2 + s3 + s4 + s5
												+ s6;
										sbf.append(str + ",");
									}
								}
							}
						}
					}
				}
			}
			break;
		}
		String str = sbf.toString();
		return str.substring(0, str.length() - 1);
	}

	/**
	 * 取得汉字的首字母 支持多音字，生成方式如（重当参:cdc,zds,cds,zdc）
	 * 
	 * @param chines
	 * @param isLowerCase
	 *            true时为小写，false时为大写
	 * @return 拼音
	 */
	public static String[] getFirstSpell(String chinese,Boolean isLowerCase,Boolean isDeleteSpecialCharacter) {
		if ("".equals(chinese) || chinese == null) {
			return null;
		}
		if (isLowerCase == null) {
			isLowerCase = true;
		}
		if (isDeleteSpecialCharacter == null) {
			isDeleteSpecialCharacter = false;
		}
		StringBuffer pinyinName = new StringBuffer();
		char[] nameChar = chinese.toCharArray();
		HanyuPinyinOutputFormat defaultFormat = new HanyuPinyinOutputFormat();
		if (isLowerCase) {
			defaultFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);// 设置小写
		} else {
			defaultFormat.setCaseType(HanyuPinyinCaseType.UPPERCASE);// 设置大写
		}

		defaultFormat.setVCharType(HanyuPinyinVCharType.WITH_V);// 设置用v代替ü
		defaultFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);// 设置不显示声调
		for (int i = 0; i < nameChar.length; i++) {
			if (nameChar[i] > 128) {
				try {
					// 取得当前汉字的所有全拼
					String[] strs = PinyinHelper.toHanyuPinyinStringArray(
							nameChar[i], defaultFormat);
					if (strs != null) {
						for (int j = 0; j < strs.length; j++) {
							// 取首字母
							pinyinName.append(strs[j].charAt(0));
							if (j != strs.length - 1) {
								pinyinName.append(",");
							}
						}
					}
				} catch (BadHanyuPinyinOutputFormatCombination e) {
					e.printStackTrace();
				}
			} else {
				if (isDeleteSpecialCharacter == true) {
					String temp = Character.toString(nameChar[i]);
					if (!temp // 判断是否为特殊字符
							.matches("[`~!@#$^&*()=|{}':;',\\[\\].<>/?~！@#￥……&*（）——|{}【】‘；：”“'。，、？]")) {
						pinyinName.append(temp);
					}
				} else {
					pinyinName.append(nameChar[i]);
				}
			}
			pinyinName.append(" ");
		}
		List<String> list = parseTheChineseByObject(discountTheChinese(pinyinName
				.toString()));
		return list.toArray(new String[list.size()]);
	}

	/**
	 * 将汉字简单转换为拼音，多音字默认采取第一个
	 * 
	 * @param chinese
	 *            需要转换的文字字符串
	 * @param separator
	 *            每个汉字拼音之间的分隔符,为空时无分隔符（例如张三:zhang-san）
	 * @param
	 * @return
	 * */
	public static String getSimplePinYin(String chinese, String separator,
			Boolean isDeleteSpecialCharacter) {
		if ("".equals(chinese) || chinese == null) {
			return null;
		}
		if (separator == null) {
			separator = "";
		}
		if (isDeleteSpecialCharacter == null) {
			isDeleteSpecialCharacter = false;
		}
		HanyuPinyinOutputFormat defaultFormat = new HanyuPinyinOutputFormat();
		defaultFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);// 设置小写
		defaultFormat.setVCharType(HanyuPinyinVCharType.WITH_V);// 设置用v代替ü
		defaultFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);// 设置不显示声调
		// 将字符串转换为char[]
		char c[] = chinese.toCharArray();
		// 定义接收所有拼音的一个StringBuffer
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < c.length; i++) {
			// 判断是否为汉字
			if (Character.toString(c[i]).matches("[\\u4E00-\\u9FA5]+")) {
				try {
					// 将一个汉字转换为String数组
					String[] temp = PinyinHelper.toHanyuPinyinStringArray(c[i],
							defaultFormat);
					buffer.append(temp[0]);
				} catch (BadHanyuPinyinOutputFormatCombination e) {
					e.printStackTrace();
				}

			} else {
				if (isDeleteSpecialCharacter == true) {
					String temp = Character.toString(c[i]);
					if (!temp // 判断是否为特殊字符
							.matches("[`~!@#$^&*()=|{}':;',\\[\\].<>/?~！@#￥……&*（）——|{}【】‘；：”“'。，、？]")) {
						buffer.append(temp);
					}
				} else {
					buffer.append(c[i]);
				}
			}
			if (i != c.length - 1) { // 除了最后一个汉字的拼音以外，其它都加入分隔符
				buffer.append(separator);// 追加分隔符
			}
		}
		return buffer.toString();
	}
}
