public class Test {
	public static void main(String[] args) throws Exception {
	    new Test().run();
	}
	public void run() throws Exception {
		System.out.println("Started");
		while (flag) {
			Thread.sleep(1000);
			int i = Math.max(3,2);
			;
		}
    }
	public static boolean flag = true;
	public static Object o = new Object();
}
