var pieData = [{value: 6, color: "#4caf50", label: "Passed"},{value: 1, color: "#f44336", label: "Failed"}];
var pieOptions = {segmentShowStroke: false,animateScale: true};
var countries = document.getElementById("pie-report").getContext("2d");
new Chart(countries).Pie(pieData, pieOptions);
