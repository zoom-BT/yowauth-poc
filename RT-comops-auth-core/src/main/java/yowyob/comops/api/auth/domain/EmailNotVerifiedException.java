package yowyob.comops.api.auth.domain;

/**
 * Levée quand un compte LOCAL tente de se connecter / d'obtenir une session alors que son
 * email n'a pas encore été vérifié. Le kernel reste seul juge : tant que l'email n'est pas
 * confirmé, aucune session utilisable n'est délivrée (mode strict).
 */
public class EmailNotVerifiedException extends RuntimeException {

    private final String email;

    public EmailNotVerifiedException(String email) {
        super("Email address is not verified.");
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
