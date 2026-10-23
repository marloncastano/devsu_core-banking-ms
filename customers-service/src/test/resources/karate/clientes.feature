Feature: Clientes (/api/clientes) - CRUD

  Background:
    * url baseUrl
    * def randomIdentification = function() { return '' + (1000000000 + Math.floor(Math.random() * 8999999999)) }
    * def identification = randomIdentification()

  Scenario: Caso de uso 1 - crear, consultar, actualizar y eliminar a Jose Lema
    Given path 'api', 'clientes'
    And request
      """
      {
        "name": "Jose Lema",
        "gender": "MALE",
        "birthDate": "1985-04-12",
        "identification": "#(identification)",
        "address": "Otavalo sn y principal",
        "phone": "098254785",
        "password": "1234"
      }
      """
    When method post
    Then status 201
    And match responseHeaders['Location'][0] contains '/api/clientes/'
    And match response == { customerId: '#uuid', name: 'Jose Lema', gender: 'MALE', birthDate: '1985-04-12', age: '#number', identification: '#(identification)', address: 'Otavalo sn y principal', phone: '098254785', active: true }
    And match response !contains { password: '#notnull' }
    * def customerId = response.customerId

    Given path 'api', 'clientes', customerId
    When method get
    Then status 200
    And match response.name == 'Jose Lema'

    Given path 'api', 'clientes', customerId
    And request { address: 'Calle 10', phone: '0991112233' }
    When method patch
    Then status 200
    And match response contains { address: 'Calle 10', phone: '0991112233', identification: '#(identification)' }

    Given path 'api', 'clientes', customerId
    When method delete
    Then status 204

    Given path 'api', 'clientes', customerId
    When method get
    Then status 200
    And match response.active == false

  Scenario: Listado paginado
    Given path 'api', 'clientes'
    And param page = 0
    And param size = 5
    When method get
    Then status 200
    And match response == { content: '#array', page: 0, size: 5, totalElements: '#number', totalPages: '#number' }

  Scenario: Una persona no puede registrarse dos veces con la misma identificación
    * def customer = { name: 'Marianela Montalvo', gender: 'FEMALE', birthDate: '1990-08-25', identification: '#(identification)', address: 'Amazonas y NNUU', phone: '097548965', password: '5678' }

    Given path 'api', 'clientes'
    And request customer
    When method post
    Then status 201

    Given path 'api', 'clientes'
    And request customer
    When method post
    Then status 409
    And match response.detail == 'A person with identification ' + identification + ' already exists'

  Scenario: Datos inválidos devuelven 400 con el detalle de cada campo
    Given path 'api', 'clientes'
    And request { name: '', gender: 'MALE', birthDate: '2030-01-01', identification: '12', address: 'x', phone: 'abc', password: '1' }
    When method post
    Then status 400
    And match response.errors contains { name: '#string', birthDate: '#string', identification: '#string', phone: '#string', password: '#string' }

  Scenario: La identificación y la fecha de nacimiento son inmutables
    Given path 'api', 'clientes'
    And request { name: 'Juan Osorio', gender: 'MALE', birthDate: '1988-01-30', identification: '#(identification)', address: '13 junio y Equinoccial', phone: '098874587', password: '1245' }
    When method post
    Then status 201
    * def customerId = response.customerId

    Given path 'api', 'clientes', customerId
    And request { birthDate: '2000-01-01' }
    When method patch
    Then status 400
    And match response.detail == "Unknown field 'birthDate'"

  Scenario: Un cliente inexistente devuelve 404
    Given path 'api', 'clientes', '00000000-0000-0000-0000-000000000000'
    When method get
    Then status 404
