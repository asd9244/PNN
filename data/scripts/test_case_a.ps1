$body = @{
    drugId = 1262
    supplements = @(
        @{
            name = "test"
            nutrients = @(
                @{ name = "Vitamin C"; amount = 500; unit = "mg" }
            )
        }
    )
} | ConvertTo-Json -Depth 5

$response = Invoke-RestMethod -Uri "http://localhost:8080/api/interaction/check" -Method Post -ContentType "application/json" -Body $body
$response | ConvertTo-Json -Depth 5
