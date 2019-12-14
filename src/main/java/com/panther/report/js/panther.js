var pieData = <<analyticsData>>;
var pieOptions = {segmentShowStroke: false,animateScale: true}
var countries = document.getElementById("pie-report").getContext("2d");
new Chart(countries).Pie(pieData, pieOptions);