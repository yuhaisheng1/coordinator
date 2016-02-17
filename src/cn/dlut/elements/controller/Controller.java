package cn.dlut.elements.controller;


public class Controller {
	
	private String ip;
	private String port;
	
	public Controller(final String ipAddress, final String port) {
        this.ip = ipAddress;
        this.port = port;
    }
	public Controller()
	{
		this.ip=null;
		this.port=null;
	}
	
	public String toSimpleString() {
        return ip+":"+port;
    }
	
	public void setIp(final String ipAddress){
		this.ip = ipAddress;
	}
	
	public void setPort(final String port){
		this.port = port;
	}

	public String getIp() {
		return ip;
	}

	public String getPort() {
		return port;
	}
	
	
	/*public boolean equals(Controller c){
		if(c.getIp().equals(this.getIp()) && c.getPort().equals(this.getPort()))
			return true;
		else 
			return false;
	}*/
	

	
	@Override
	public String toString()
	{
		return this.getIp()+":"+this.getPort();
	}

}


