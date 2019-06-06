package mvc.action;

import annotation.*;

@JService
public class DemoServiceImpl implements IDemoservice{

	public String get(String name) {
		// TODO Auto-generated method stub
		return "My name is " + name;
	}

}
