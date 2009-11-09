public class ScopedAccess {
	public static void main(String[] args) throws Exception {
	    new ScopedAccess().run();
	}
	public void run() throws Exception {
		System.out.println("Started");
		while (flag) {
			Thread.sleep(100);
            int a = 0;
			int i = Math.max(a,2);
			System.out.println(i);
		}
    }
	public static boolean flag = true;
	public Object o = new Object() {
	    public int answer(int x) { return 42+x; }
	    public String toString() { return "custom tostring"; }
    };
}
