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

		throw new IllegalArgumentException("trying an exception breakpoint");
    }
	public static boolean flag = true;
	public static Object o = new Object() {
	    public int answer(int x) { return 42+x; }
	    public String toString() { return "custom tostring"; }
    };
}
