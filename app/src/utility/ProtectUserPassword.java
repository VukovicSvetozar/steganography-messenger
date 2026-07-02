package utility;

public class ProtectUserPassword {

	public static void main(String[] args) {

		String myPassword = "eliw";

		String salt = PasswordUtils.getSalt(30);

		String mySecurePassword = PasswordUtils.generateSecurePassword(myPassword, salt);

		System.out.println("Sigurna lozinka = " + mySecurePassword);
		System.out.println("Salt = " + salt);
	}
}
