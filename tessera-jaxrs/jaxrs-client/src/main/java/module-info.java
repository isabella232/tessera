module tessera.jaxrs.client {
  requires java.ws.rs;
  requires tessera.config;
  requires tessera.security.main;
  requires tessera.shared.main;
  requires tessera.tessera.context.main;

  exports com.quorum.tessera.jaxrs.client;

  provides com.quorum.tessera.context.RestClientFactory with
      com.quorum.tessera.jaxrs.client.ClientFactory;

  uses com.quorum.tessera.ssl.context.SSLContextFactory;
  uses com.quorum.tessera.ssl.context.ClientSSLContextFactory;
}
