function fn() {
  // baseUrl is injected by CustomersKarateTest; the default allows running against a local instance.
  return {
    baseUrl: karate.properties['baseUrl'] || 'http://localhost:8081'
  };
}
