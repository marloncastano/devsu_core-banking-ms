Feature: Casos de uso del enunciado - cuentas, movimientos y reporte

  Background:
    * url baseUrl
    * def jose = CustomerEvents.created('Jose Lema')
    * def marianela = CustomerEvents.created('Marianela Montalvo')
    * def juan = CustomerEvents.created('Juan Osorio')
    * def open = function(customerId, type, balance) { return karate.call('classpath:karate/common/open-account.feature', { customerId: customerId, accountType: type, initialBalance: balance }).accountNumber }
    * def move = function(account, amount, expected) { karate.call('classpath:karate/common/register-movement.feature', { accountNumber: account, amount: amount, expectedBalance: expected }) }

  Scenario: Casos de uso 2 a 5 - cuentas, movimientos, F3 y reporte de estado de cuenta (F4)
    # Casos de uso 2 y 3: apertura de cuentas
    * def joseSavings = open(jose, 'SAVINGS', 2000)
    * def marianelaChecking = open(marianela, 'CHECKING', 100)
    * def juanSavings = open(juan, 'SAVINGS', 0)
    * def marianelaSavings = open(marianela, 'SAVINGS', 540)
    * def joseChecking = open(jose, 'CHECKING', 1000)

    # Caso de uso 4: movimientos (F2)
    * move(joseSavings, -575, 1425)
    * move(marianelaChecking, 600, 700)
    * move(juanSavings, 150, 150)
    * move(marianelaSavings, -540, 0)

    # F3: retiro sin saldo
    Given path 'api', 'movimientos'
    And request { accountNumber: '#(marianelaSavings)', amount: -1 }
    When method post
    Then status 422
    And match response.detail == 'Saldo no disponible'

    # Caso de uso 5 / F4: estado de cuenta de Marianela Montalvo
    * def today = java.time.LocalDate.now() + ''
    Given path 'api', 'reportes'
    And param fecha = today + ',' + today
    And param cliente = marianela
    When method get
    Then status 200
    And match response.customerName == 'Marianela Montalvo'
    And match response.accounts == '#[2]'
    And match response.accounts contains deep { accountNumber: '#(marianelaChecking)', initialBalance: 100, availableBalance: 700, movements: [{ movementType: 'DEPOSIT', amount: 600, balance: 700 }] }
    And match response.accounts contains deep { accountNumber: '#(marianelaSavings)', initialBalance: 540, availableBalance: 0, movements: [{ movementType: 'WITHDRAWAL', amount: -540, balance: 0 }] }

  Scenario: Un reintento con la misma Idempotency-Key no mueve el dinero dos veces
    * def account = open(jose, 'SAVINGS', 1000)
    * def key = java.util.UUID.randomUUID() + ''

    Given path 'api', 'movimientos'
    And header Idempotency-Key = key
    And request { accountNumber: '#(account)', amount: -300 }
    When method post
    Then status 201
    And match responseHeaders['Idempotent-Replayed'][0] == 'false'
    * def movementId = response.movementId

    Given path 'api', 'movimientos'
    And header Idempotency-Key = key
    And request { accountNumber: '#(account)', amount: -300 }
    When method post
    Then status 201
    And match responseHeaders['Idempotent-Replayed'][0] == 'true'
    And match response.movementId == movementId

    Given path 'api', 'cuentas', account
    When method get
    Then status 200
    And match response.availableBalance == 700

  Scenario: Una cuenta desactivada o un cliente desactivado no pueden mover dinero
    * def account = open(juan, 'CHECKING', 100)

    Given path 'api', 'cuentas', account
    And request { active: false }
    When method patch
    Then status 200

    Given path 'api', 'movimientos'
    And request { accountNumber: '#(account)', amount: 10 }
    When method post
    Then status 422
    And match response.detail == 'Account ' + account + ' is inactive'

    * CustomerEvents.deactivated(juan, 'Juan Osorio')
    Given path 'api', 'cuentas'
    And request { accountType: 'SAVINGS', initialBalance: 0, customerId: '#(juan)' }
    When method post
    Then status 422
    And match response.detail == 'Customer ' + juan + ' is inactive'

  Scenario: El reporte valida el formato del rango de fechas
    Given path 'api', 'reportes'
    And param fecha = '10/2/2022'
    And param cliente = jose
    When method get
    Then status 400
    And match response.errors.fecha == 'must have the format yyyy-MM-dd,yyyy-MM-dd'
