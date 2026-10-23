@ignore
Feature: Reusable step - register a movement and check the resulting balance

  Scenario:
    Given url baseUrl
    And path 'api', 'movimientos'
    And request { accountNumber: '#(accountNumber)', amount: '#(amount)' }
    When method post
    Then status 201
    And match response.movementType == (amount > 0 ? 'DEPOSIT' : 'WITHDRAWAL')
    And match response.balance == expectedBalance
