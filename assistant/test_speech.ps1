Add-Type -AssemblyName System.Speech

$recognizer = New-Object System.Speech.Recognition.SpeechRecognitionEngine
$recognizer.SetInputToDefaultAudioDevice()

$grammar = New-Object System.Speech.Recognition.DictationGrammar
$recognizer.LoadGrammar($grammar)

$recognizer.RecognizeTimeout = [TimeSpan]::FromSeconds(5)

Write-Host "Ready - Please speak now..."
$result = $recognizer.Recognize()

if ($result) {
    Write-Host "Recognized: $($result.Text)"
} else {
    Write-Host "No speech detected"
}

$recognizer.Dispose()
