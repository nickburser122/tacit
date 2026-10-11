#!/usr/bin/env bash
adb logcat -c
status=0
: > device-tests.log
: > tests-ran.txt
rm -f timeout-diagnostics.txt
for cls in ${1//,/ }; do
  timeout 420 ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class="$cls" --stacktrace >> device-tests.log 2>&1
  code=$?
  if [ "$code" -ne 0 ]; then
    status=1
    if [ "$code" -eq 124 ]; then
      echo "TIMEOUT in $cls" >> tests-ran.txt
      adb logcat -d -v threadtime > logcat-timeout.txt
      {
        adb shell dumpsys activity activities | grep -E "mResumedActivity|topResumedActivity"
        adb shell dumpsys window windows | grep -E "mCurrentFocus|mFocusedApp"
      } >> timeout-diagnostics.txt 2>&1
    fi
  fi
  find app/build/outputs/androidTest-results -name 'TEST-*.xml' 2>/dev/null | while read -r f; do
    grep -oE '<testsuite [^>]*>' "$f" | head -1
    grep -oE '<testcase [^>]*>' "$f"
    grep -A6 '<failure' "$f" | cut -c1-300
  done >> tests-ran.txt
done
adb logcat -d -v threadtime > logcat.txt
{
  echo "exit status: $status"
  echo "--- crash in logcat"
  grep -n -A45 "FATAL EXCEPTION" logcat.txt | head -150
  echo "--- instrumentation output"
  grep -nE "Process crashed|FAILED|AssertionError|Exception|Tests on|tests completed|> Task :app:connected" device-tests.log | head -60
  echo "--- clipboard service lines"
  grep -iE "clipboard" logcat.txt | grep -iE "deny|denied|not focused|access" | head -20
  echo "--- test runner events"
  grep -E "TestRunner|AndroidJUnitRunner" logcat.txt | cut -c1-200 | tail -60
  echo "--- timeout diagnostics"
  cat timeout-diagnostics.txt 2>/dev/null
  echo "--- tests that ran"
  cat tests-ran.txt
} > device-summary.txt
msg=$(head -c 30000 device-summary.txt)
msg="${msg//'%'/'%25'}"
msg="${msg//$'\r'/'%0D'}"
msg="${msg//$'\n'/'%0A'}"
if [ "$status" -eq 0 ]; then
  echo "::notice title=Device tests passed::$msg"
else
  echo "::error title=Device tests failed::$msg"
fi
exit "$status"
