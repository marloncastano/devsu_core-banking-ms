function fn() {
  // baseUrl is injected by AccountsKarateTest; the default allows running against a local instance.
  return {
    baseUrl: karate.properties['baseUrl'] || 'http://localhost:8082',
    CustomerEvents: Java.type('com.devsu.accounts.karate.CustomerEvents')
  };
}
