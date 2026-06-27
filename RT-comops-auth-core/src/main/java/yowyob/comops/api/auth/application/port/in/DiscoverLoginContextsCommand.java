package yowyob.comops.api.auth.application.port.in;

public record DiscoverLoginContextsCommand(String principal, String password) {
}
