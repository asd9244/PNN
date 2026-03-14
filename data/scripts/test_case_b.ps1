$body = @{ drugIds = @(1262, 1264) } | ConvertTo-Json
$response = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/recommendations/safe-nutrients" -Method Post -ContentType "application/json" -Body $body
$response | ConvertTo-Json -Depth 5
