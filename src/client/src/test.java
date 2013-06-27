public class test {

    public static void main(String[] args) {
	
	String host	 = "169.254.75.127";
        SCClient client1 = new SCClient("user1", null, host);

        if(client1.isInitialized()) {
            System.out.println("Clients inited");
        } else {
            System.out.println("Failed to init clients");
        }
    }

}
