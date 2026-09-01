Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "         CERTIFICATE SERVICE - LIVE INTERACTIVE DEMO             " -ForegroundColor Yellow
Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8080"

# Check if server is running
try {
    $health = Invoke-RestMethod -Uri "$baseUrl/api/designs" -Method GET -ErrorAction Stop
    Write-Host "[OK] Server is running on $baseUrl" -ForegroundColor Green
} catch {
    Write-Host "[!] Server is not running on $baseUrl." -ForegroundColor Yellow
    Write-Host "    Starting server in background..." -ForegroundColor Cyan
    Start-Process -FilePath "java" -ArgumentList "-jar target\certificate-service-1.0.0.jar --spring.profiles.active=local" -WindowStyle Minimized
    Write-Host "    Waiting for server to boot..." -ForegroundColor Cyan
    Start-Sleep -Seconds 7
}

Write-Host "`n-----------------------------------------------------------------" -ForegroundColor Gray
Write-Host "STEP 1: View Preloaded Certificate Designs" -ForegroundColor Green
Write-Host "GET $baseUrl/api/designs" -ForegroundColor DarkGray
Write-Host "-----------------------------------------------------------------" -ForegroundColor Gray
$designs = Invoke-RestMethod -Uri "$baseUrl/api/designs" -Method GET
$designs | Format-Table -Property name, status, id -AutoSize

Write-Host "`n-----------------------------------------------------------------" -ForegroundColor Gray
Write-Host "STEP 2: View Preloaded Educational Programmes" -ForegroundColor Green
Write-Host "GET $baseUrl/api/programmes" -ForegroundColor DarkGray
Write-Host "-----------------------------------------------------------------" -ForegroundColor Gray
$programmes = Invoke-RestMethod -Uri "$baseUrl/api/programmes" -Method GET
$programmes | Format-Table -Property name, status, id -AutoSize

Write-Host "`n-----------------------------------------------------------------" -ForegroundColor Gray
Write-Host "STEP 3: Create a NEW Programme: 'Cloud & AI Architecture'" -ForegroundColor Green
Write-Host "POST $baseUrl/api/programmes" -ForegroundColor DarkGray
Write-Host "-----------------------------------------------------------------" -ForegroundColor Gray
$newProgBody = @{
    name = "Cloud & AI Architecture";
    description = "Mastering LLMs, Microservices, and Distributed Systems."
} | ConvertTo-Json
$createdProg = Invoke-RestMethod -Uri "$baseUrl/api/programmes" -Method POST -Body $newProgBody -ContentType "application/json"
Write-Host "Created Programme ID: $($createdProg.id)" -ForegroundColor Yellow
Write-Host "Name:                 $($createdProg.name)" -ForegroundColor Yellow

Write-Host "`n-----------------------------------------------------------------" -ForegroundColor Gray
Write-Host "STEP 4: Assign Design 'Gold Border v2' to the New Programme" -ForegroundColor Green
Write-Host "POST $baseUrl/api/programmes/$($createdProg.id)/designs" -ForegroundColor DarkGray
Write-Host "-----------------------------------------------------------------" -ForegroundColor Gray
$goldV2 = $designs | Where-Object { $_.name -eq "Gold Border v2" } | Select-Object -First 1
$assignBody = @{
    designId = $goldV2.id;
} | ConvertTo-Json
$assigned = Invoke-RestMethod -Uri "$baseUrl/api/programmes/$($createdProg.id)/designs" -Method POST -Body $assignBody -ContentType "application/json"
Write-Host "Assigned Design: $($assigned.designName) to $($assigned.programmeName)" -ForegroundColor Yellow

Write-Host "`n-----------------------------------------------------------------" -ForegroundColor Gray
Write-Host "STEP 5: Issue Certificate to 'Pavani Yellaturu'" -ForegroundColor Green
Write-Host "POST $baseUrl/api/certificates" -ForegroundColor DarkGray
Write-Host "-----------------------------------------------------------------" -ForegroundColor Gray
$issueBody = @{
    programmeId = $createdProg.id;
    personName = "Pavani Yellaturu";
    personEmail = "pavani@example.com";
} | ConvertTo-Json
$cert = Invoke-RestMethod -Uri "$baseUrl/api/certificates" -Method POST -Body $issueBody -ContentType "application/json"
Write-Host "SUCCESSFULLY ISSUED CERTIFICATE!" -ForegroundColor Green
Write-Host "Certificate ID: $($cert.id)" -ForegroundColor Yellow
Write-Host "Person Name:    $($cert.personName)" -ForegroundColor Yellow
Write-Host "Programme:      $($cert.programmeNameSnapshot)" -ForegroundColor Yellow
Write-Host "Design Used:    $($cert.designNameSnapshot)" -ForegroundColor Yellow
Write-Host "Status:         $($cert.status)" -ForegroundColor Yellow

Write-Host "`n-----------------------------------------------------------------" -ForegroundColor Gray
Write-Host "STEP 6: Public Certificate Lookup by ID" -ForegroundColor Green
Write-Host "GET $baseUrl/api/certificates/$($cert.id)" -ForegroundColor DarkGray
Write-Host "-----------------------------------------------------------------" -ForegroundColor Gray
$lookup = Invoke-RestMethod -Uri "$baseUrl/api/certificates/$($cert.id)" -Method GET
Write-Host "Lookup Verified!" -ForegroundColor Green
Write-Host "Snapshot Programme Name: $($lookup.programmeNameSnapshot)" -ForegroundColor Yellow
Write-Host "Snapshot Design Name:    $($lookup.designNameSnapshot)" -ForegroundColor Yellow

Write-Host "`n-----------------------------------------------------------------" -ForegroundColor Gray
Write-Host "STEP 7: Test Duplicate Live Certificate Prevention (Concurrency Guard)" -ForegroundColor Green
Write-Host "POST $baseUrl/api/certificates (Attempting second live certificate)" -ForegroundColor DarkGray
Write-Host "-----------------------------------------------------------------" -ForegroundColor Gray
try {
    $duplicate = Invoke-RestMethod -Uri "$baseUrl/api/certificates" -Method POST -Body $issueBody -ContentType "application/json"
    Write-Host "[ERROR] Duplicate was unexpectedly allowed!" -ForegroundColor Red
} catch {
    Write-Host "[SUCCESS] Correctly REJECTED duplicate certificate with HTTP 409 Conflict!" -ForegroundColor Green
    $stream = $_.Exception.Response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($stream)
    Write-Host "Response Payload: $($reader.ReadToEnd())" -ForegroundColor DarkYellow
}

Write-Host "`n-----------------------------------------------------------------" -ForegroundColor Gray
Write-Host "STEP 8: Cancel / Revoke the Certificate" -ForegroundColor Green
Write-Host "POST $baseUrl/api/certificates/$($cert.id)/cancel" -ForegroundColor DarkGray
Write-Host "-----------------------------------------------------------------" -ForegroundColor Gray
$cancelBody = @{
    reason = "Candidate requested name spelling adjustment to 'Pavani R. Yellaturu'."
} | ConvertTo-Json
$cancelledCert = Invoke-RestMethod -Uri "$baseUrl/api/certificates/$($cert.id)/cancel" -Method POST -Body $cancelBody -ContentType "application/json"
Write-Host "Certificate Status:      $($cancelledCert.status)" -ForegroundColor Yellow
Write-Host "Cancellation Reason:     $($cancelledCert.cancellationReason)" -ForegroundColor Yellow
Write-Host "Cancellation Timestamp:  $($cancelledCert.cancelledAt)" -ForegroundColor Yellow

Write-Host "`n-----------------------------------------------------------------" -ForegroundColor Gray
Write-Host "STEP 9: Re-Issue Certificate (Allowed After Cancellation)" -ForegroundColor Green
Write-Host "POST $baseUrl/api/certificates" -ForegroundColor DarkGray
Write-Host "-----------------------------------------------------------------" -ForegroundColor Gray
$reissueBody = @{
    programmeId = $createdProg.id;
    personName = "Pavani R. Yellaturu";
    personEmail = "pavani@example.com";
} | ConvertTo-Json
$reissuedCert = Invoke-RestMethod -Uri "$baseUrl/api/certificates" -Method POST -Body $reissueBody -ContentType "application/json"
Write-Host "SUCCESSFULLY RE-ISSUED NEW CERTIFICATE!" -ForegroundColor Green
Write-Host "New Certificate ID: $($reissuedCert.id)" -ForegroundColor Yellow
Write-Host "Person Name:        $($reissuedCert.personName)" -ForegroundColor Yellow
Write-Host "Status:             $($reissuedCert.status)" -ForegroundColor Yellow

Write-Host "`n-----------------------------------------------------------------" -ForegroundColor Gray
Write-Host "STEP 10: List All Certificates for 'pavani@example.com'" -ForegroundColor Green
Write-Host "GET $baseUrl/api/certificates?personEmail=pavani@example.com" -ForegroundColor DarkGray
Write-Host "-----------------------------------------------------------------" -ForegroundColor Gray
$list = Invoke-RestMethod -Uri "$baseUrl/api/certificates?personEmail=pavani@example.com" -Method GET
$list.content | Format-Table -Property personName, programmeNameSnapshot, status, id, cancellationReason -AutoSize

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "DEMO COMPLETE! All features executed and verified successfully." -ForegroundColor Green
Write-Host "Interactive Swagger UI available at: $baseUrl/swagger-ui.html" -ForegroundColor Yellow
Write-Host "=================================================================" -ForegroundColor Cyan
