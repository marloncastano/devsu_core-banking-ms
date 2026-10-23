@ignore
Feature: Reusable step - open an account and return its number

  Scenario:
    Given url baseUrl
    And path 'api', 'cuentas'
    And request { accountType: '#(accountType)', initialBalance: '#(initialBalance)', customerId: '#(customerId)' }
    When method post
    Then status 201
    And match response.availableBalance == initialBalance
    * def accountNumber = response.accountNumber
