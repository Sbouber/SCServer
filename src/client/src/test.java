public class test {

    public static void main(String[] args) {
        SCClient client1 = new SCClient("user1", null);

        if(client1.isInitialized()) {
            System.out.println("Clients inited");
        } else {
            System.out.println("Failed to init clients");
        }
    }

}
