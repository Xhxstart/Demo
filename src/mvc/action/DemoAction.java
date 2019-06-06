package mvc.action;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import annotation.*;

@XHXController
@XHXRequestMapping("/web")
public class DemoAction {
	
	@XHXAutowried private IDemoservice demoService;
	
	@XHXRequestMapping("/query")
	public void query(HttpServletRequest req, HttpServletResponse resp,
			@XHXRequestParam("name") String name){
		
		String result = demoService.get(name);

		try {
			req.setCharacterEncoding("UTF-8");
			resp.setCharacterEncoding("UTF-8");
			resp.getWriter().write(result);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	@XHXRequestMapping("/edit.json")
	public void edit(HttpServletRequest req, HttpServletResponse resp, Integer id){
		
	}
	
	@XHXRequestMapping("/remove.json")
	public void remove(HttpServletRequest req, HttpServletResponse resp, Integer id){
		
	}

}
