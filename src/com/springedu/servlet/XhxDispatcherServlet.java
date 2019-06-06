package com.springedu.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import annotation.*;

public class XhxDispatcherServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Properties properties = new Properties();
	
	private List<String> classNames = new ArrayList<String>();
	
	private Map<String,Object> ioc= new HashMap<String,Object>();
	
	private Map<String, Method> handermapping = new HashMap<String,Method>();
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO Auto-generated method stub
		this.doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO Auto-generated method stub
		// 《-----运行阶段---》
		// service方法 的doget dopost
		// 调用一个dispatch方法，根据用户请求的URL去匹配已经初始化好评的HanderMapping中的method
		 try {
			doDispatch(req, resp);
		}
		catch (Exception e) {
			if (e.getMessage() != null)
			resp.getWriter().write("500 Exception: "+ e.getMessage());
		}
	}
	
	private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		String url = req.getRequestURI();
		
		//将用户输入的URL取出来
		String contextPath = req.getContextPath();
		
		url = url.replace(contextPath, "").replaceAll("/+", "/");
		if (!handermapping.containsKey(url)) {
			resp.getWriter().write("404 Not Founded!!!!");
			return;
		}
		System.out.println("已经取到对应的方法：" + handermapping.get(url));
		// 通过Java的反射区动态调用 用户输入的方法
		// 关键是
		// 1 怎么从IOC容器(map<beanName 实例>)中取得这个URL调用的实例
		// 2我们只知道handermapping中URL对应的method
		// 3取得bean的实例利用反射调用url对应的method
		Map<String, String[]> params = req.getParameterMap();
		Method method = handermapping.get(url);
		Class<?>[] parameterTypes = method.getParameterTypes();
		Object[] paramValues = new Object[parameterTypes.length];
		for (int i = 0; i < parameterTypes.length; i++) {
			Class<?> paramType = parameterTypes[i];
			if (paramType == HttpServletRequest.class) {
				paramValues[i] = req;
			} else if (paramType == HttpServletResponse.class) {
				paramValues[i] = resp;
			}
			else if (paramType == String.class) {
				for (Map.Entry<String, String[]> entry : params.entrySet()) {
					String value = Arrays.toString(entry.getValue())
					.replaceAll("\\[|\\]", "")
					.replaceAll("\\s", "");
					paramValues[i] = value;
				}
			}
		}
		String beanName = lowerFirst(method.getDeclaringClass().getSimpleName());
		String result = (String) method.invoke(ioc.get(beanName), paramValues);
		// 通过Response输出
		resp.getWriter().write(result);
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		// <----初始化--->
		// 调用init方法 被Web容器调用，如Tomcat
		super.init(config);
		// 1加载application。xml内容、
		doLoadConfig(config.getInitParameter("contextConfigLocation"));
		// 2扫描相关联的class，个根据用户定义的包名去扫描
		doScanner(properties.getProperty("scanPackage"));
		// 3初始化所有class的实例  保存在IOC容器的初始化 就是一个map  <beanName 实例>
		doInstance();
		// 4 自动的依赖注入  如果声明类中定义了成员变量，而且需要赋值
		doAutowired();
		// 5初始化handermapping 将URL和Controller中的某个方法进行一对一关联，并保存在map中		
		initHanderMapping();
		System.out.println("初始化成功撒啊啊啊啊啊啊啊啊、aaaaa");
		
	}

	private void initHanderMapping() {
		// 关联用户请求handermapping中配置的信息和Method进行关联，并保存这些关系。
		if (ioc.isEmpty()) {
			return;
		}
		for (Entry<String,Object> entry : ioc.entrySet()) {
			Class<?> clazz = entry.getValue().getClass();
			if (!clazz.isAnnotationPresent(XHXController.class)) {
				continue;
			}
			 StringBuilder baseUrl = new StringBuilder("/");
			if (clazz.isAnnotationPresent(XHXRequestMapping.class)) {
				XHXRequestMapping requestmapping = clazz.getAnnotation(XHXRequestMapping.class);
				baseUrl.append(requestmapping.value());
			}
			
			Method[] methods = clazz.getMethods();
			for (Method method : methods) {
				if (!method.isAnnotationPresent(XHXRequestMapping.class)) {
					continue;
				}
				XHXRequestMapping requestmapping = method.getAnnotation(XHXRequestMapping.class);
				//baseUrl.append("/" + requestmapping.value());
				String url = (baseUrl.toString() + requestmapping.value()).replaceAll("/+", "/") ;
				handermapping.put(url, method);
				System.out.println("url:" + url + "," + method);
			}
 		}
		
		
	}

	private void doAutowired() {
		//如果IOC容器為空的時候
		if (ioc.isEmpty()) { 
			return ;
		}
		
		// 不为空
		for (Entry<String ,Object> entry: ioc.entrySet()) {
			// 在spring里面没有隐私，所有成员变量都需要注入，包括私有的（有注解的情况下）
			// 利用java反射取得所有成员变量
			Field[] fields = entry.getValue().getClass().getDeclaredFields();
			for (Field field : fields) {
				// 是不是加了自动注入的注解
				if (!field.isAnnotationPresent(XHXAutowried.class)) {
					continue;
				}
				XHXAutowried autowried = field.getAnnotation(XHXAutowried.class);
				String beanName = autowried.value().trim();
				if ("".equals(beanName)) {
					beanName = field.getType().getName();
				}
				// 暴力访问  设置可访问
				field.setAccessible(true);
				try {
					field.set(entry.getValue(), ioc.get(beanName));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					continue;
				} 
			}
		}
		
	}

	private void doInstance() {
		if (classNames.isEmpty()) { 
			return ;
		}
		try {
			for (String classname : classNames) {
				Class clazz = Class.forName(classname);
				// 进行实例化 原则问题： 不是所有的类都要实例化
				if (clazz.isAnnotationPresent(XHXController.class)) {
					String beanName = lowerFirst(clazz.getSimpleName());
					ioc.put(beanName, clazz.newInstance());
					
				} else if (clazz.isAnnotationPresent(JService.class)) {
					// 1在容器里面有个beanID 默认采用类名 的小写
					// 2如果自己定义了的话，优先使用自定义的
					// 3 根据类型匹配，利用接口名（应该是多个接口）作为key
					JService service = (JService) clazz.getAnnotation(JService.class);
					String beanName = service.value();
					// 判断这个接口有没有设置注解，如果为空就使用默认的首字母小写
					if (!"".equals(beanName)) {
						  ioc.put(beanName, clazz.newInstance());
					}
					// 如果自己定义了的话，优先使用自定义的
					Class<?>[] interfaces = clazz.getInterfaces();
					for (Class<?> i : interfaces) {
						ioc.put(i.getName(), clazz.newInstance());
					}
					 
				} else {
					continue;
				}
			}
		} catch (Exception e){
			
		}
		
	}

	private void doLoadConfig(String location) {
		InputStream is = null;
		is = getServletContext().getResourceAsStream(location);
		try {
			properties.load(is);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (is != null) {
					is.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void doScanner(String packageName) {
		// com.xhx.demo -> com/xhx/demo
		URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
		File classDir = new File(url.getFile());
		for (File file : classDir.listFiles()) {
			if (file.isDirectory()) {
				doScanner(packageName + "." + file.getName());
			} else {				
				classNames.add(packageName + "." + file.getName().replace(".class", ""));
			}
		}

	}
	
	private String lowerFirst(String str) {
		char[] chars = str.toCharArray();
		chars[0] += 32;
		return String.valueOf(chars);
	}
	
}
