Feature: Criterios de aceptación de la API de indicadores

  Background:
    * url 'http://localhost:8080'

  # ---------------------------------------------------------------------
  # /series
  # ---------------------------------------------------------------------

  Scenario: Serie rechaza la ausencia del parámetro country
    Given path 'api/indicators/series'
    And param indicator = 'SP.POP.TOTL'
    When method get
    Then status 400

  Scenario: Serie rechaza la ausencia del parámetro indicator
    Given path 'api/indicators/series'
    And param country = 'BRA'
    When method get
    Then status 400

  Scenario: Serie rechaza un country en blanco
    Given path 'api/indicators/series'
    And param country = ''
    And param indicator = 'SP.POP.TOTL'
    When method get
    Then status 400

  Scenario: Serie devuelve 200 con los puntos esperados
    Given path 'api/indicators/series'
    And param country = 'BRA'
    And param indicator = 'SP.POP.TOTL'
    And param from = 2010
    And param to = 2020
    When method get
    Then status 200
    And match response.countryIso3 == 'BRA'
    And match response.indicatorId == 'SP.POP.TOTL'
    And match response.points == '#[11]'
    And match each response.points == { year: '#number', value: '#number' }

  Scenario: Serie devuelve 404 cuando el país no reporta el indicador
    Given path 'api/indicators/series'
    And param country = 'PRK'
    And param indicator = 'NY.GDP.MKTP.CD'
    When method get
    Then status 404

  # ---------------------------------------------------------------------
  # /gdp-per-capita
  # ---------------------------------------------------------------------

  Scenario: PIB per cápita rechaza la ausencia del parámetro country
    Given path 'api/indicators/gdp-per-capita'
    And param year = 2022
    When method get
    Then status 400

  Scenario: PIB per cápita rechaza la ausencia del parámetro year
    Given path 'api/indicators/gdp-per-capita'
    And param country = 'COL'
    When method get
    Then status 400

  Scenario: PIB per cápita rechaza un año anterior a 1960
    Given path 'api/indicators/gdp-per-capita'
    And param country = 'COL'
    And param year = 1900
    When method get
    Then status 400

  Scenario: PIB per cápita devuelve 200 sin conversión de moneda
    Given path 'api/indicators/gdp-per-capita'
    And param country = 'COL'
    And param year = 2022
    When method get
    Then status 200
    And match response.countryIso3 == 'COL'
    And match response.year == 2022
    And match response.gdpPerCapitaUsd == '#number'
    And match response.targetCurrency == '#null'

  Scenario: PIB per cápita devuelve 200 con conversión a otra moneda
    Given path 'api/indicators/gdp-per-capita'
    And param country = 'COL'
    And param year = 2022
    And param currency = 'EUR'
    When method get
    Then status 200
    And match response.targetCurrency == 'EUR'
    And match response.gdpPerCapitaConverted == '#number'
    And match response.exchangeRate == '#number'

  Scenario: PIB per cápita devuelve 404 cuando la moneda destino no está soportada
    Given path 'api/indicators/gdp-per-capita'
    And param country = 'COL'
    And param year = 2022
    And param currency = 'COP'
    When method get
    Then status 404

  Scenario: PIB per cápita devuelve 404 cuando el país no reporta PIB o población
    Given path 'api/indicators/gdp-per-capita'
    And param country = 'PRK'
    And param year = 2022
    When method get
    Then status 404

  # ---------------------------------------------------------------------
  # /ranking
  # ---------------------------------------------------------------------

  Scenario: Ranking rechaza la ausencia del parámetro indicator
    Given path 'api/indicators/ranking'
    And param year = 2022
    When method get
    Then status 400

  Scenario: Ranking rechaza la ausencia del parámetro year
    Given path 'api/indicators/ranking'
    And param indicator = 'NY.GDP.MKTP.CD'
    When method get
    Then status 400

  Scenario: Ranking rechaza un año anterior a 1960
    Given path 'api/indicators/ranking'
    And param indicator = 'NY.GDP.MKTP.CD'
    And param year = 1900
    When method get
    Then status 400

  Scenario: Ranking rechaza un límite menor a 1
    Given path 'api/indicators/ranking'
    And param indicator = 'NY.GDP.MKTP.CD'
    And param year = 2022
    And param limit = 0
    When method get
    Then status 400

  Scenario: Ranking rechaza un límite fuera de rango
    Given path 'api/indicators/ranking'
    And param indicator = 'NY.GDP.MKTP.CD'
    And param year = 2022
    And param limit = 500
    When method get
    Then status 400

  Scenario: Ranking devuelve 200 con la lista esperada
    Given path 'api/indicators/ranking'
    And param indicator = 'NY.GDP.MKTP.CD'
    And param year = 2022
    And param limit = 10
    When method get
    Then status 200
    And match response == '#[10]'
    And match response[0].position == 1
    And match each response == { position: '#number', countryIso3: '#string', countryName: '#string', value: '#number' }

  Scenario: Ranking devuelve 404 cuando el indicador no tiene datos para ese año
    Given path 'api/indicators/ranking'
    And param indicator = 'IT.NET.USER.ZS'
    And param year = 1960
    And param limit = 10
    When method get
    Then status 404

  # ---------------------------------------------------------------------
  # /exchange-rate
  # ---------------------------------------------------------------------

  Scenario: Convertir rechaza la ausencia del parámetro from
    Given path 'api/indicators/exchange-rate'
    And param to = 'USD'
    When method get
    Then status 400

  Scenario: Convertir rechaza la ausencia del parámetro to
    Given path 'api/indicators/exchange-rate'
    And param from = 'USD'
    When method get
    Then status 400

  Scenario: Convertir rechaza un amount igual a cero
    Given path 'api/indicators/exchange-rate'
    And param from = 'USD'
    And param to = 'USD'
    And param amount = 0
    When method get
    Then status 400

  Scenario: Convertir rechaza un amount negativo
    Given path 'api/indicators/exchange-rate'
    And param from = 'USD'
    And param to = 'USD'
    And param amount = -5
    When method get
    Then status 400

  Scenario: Convertir entre la misma moneda devuelve 200
    Given path 'api/indicators/exchange-rate'
    And param from = 'USD'
    And param to = 'USD'
    When method get
    Then status 200
    And match response.rate == 1
    And match response.convertedAmountFormatted == '$1.00'

  Scenario: Convertir entre monedas distintas devuelve 200
    Given path 'api/indicators/exchange-rate'
    And param from = 'USD'
    And param to = 'EUR'
    And param amount = 100
    When method get
    Then status 200
    And match response.rate == '#number'
    And match response.convertedAmount == '#number'

  Scenario: Convertir devuelve 404 cuando la moneda destino no está soportada
    Given path 'api/indicators/exchange-rate'
    And param from = 'USD'
    And param to = 'COP'
    When method get
    Then status 404
