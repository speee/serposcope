/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */

/* global serposcope, Slick */

serposcope.googleTargetController = function () {

    var HEADER_SIZE = 100;

    var resize = function () {
        var height = serposcope.theme.availableHeight() - HEADER_SIZE;
        $('#google-target-variation-container').css("min-height", (height) + "px");
        $('#google-target-chart').css("height", (height) + "px");
        renderChart();
        serposcope.googleTargetControllerGrid.resize(height);
        serposcope.googleTargetControllerVariation.resize();
    };

    var maxRank = 50;
    var maxRankOffset = 5;
    var minRank = 0;
    var minRankOffset = -5;
    var chart = null;
    var chartData = [];
    var chartStartDate = null;
    var chartOptions = {
        labels: ["Date", "###CALENDAR###"],
        legend: "always",
        visibility: [true],
        labelsDivWidth: "100%",
        labelsDiv: "google-target-legend",
        labelsUTC: true,
        drawPoints: true,
        connectSeparatedPoints: true,
        pointSize: 2,
        highlightSeriesBackgroundAlpha: 0.8,
        highlightSeriesOpts: {strokeWidth: 2},
        drawHighlightPointCallback: function (g, seriesName, canvasContext, cx, cy, color, pointSize) {},
        axes: {
            x: {
                axisLabelWidth: 120,
                pixelsPerLabel: 80,
                axisLabelFontSize: 12,
                axisLabelFormatter: function (d, gran) {
                    return new Date(chartStartDate.getTime() + d * 24 * 60 * 60 * 1000).format("yyyy-mm-dd");
                },
                valueFormatter: function (d) {
                    return "<strong>" + new Date(chartStartDate.getTime() + d * 24 * 60 * 60 * 1000).format("yyyy-mm-dd") + "</strong>";
                }
            },
            y: {
                axisLabelWidth: 20,
                axisLabelFontSize: 12,
                valueFormatter: function (y) {
                    return "<strong>" + y + "</strong>";
                }
            }
        },
        valueRange: [maxRank + maxRankOffset, minRank + minRankOffset]
    };

    var renderChart = function () {
        if (document.getElementById("google-target-chart") == null) {
            $('.calendar-annotation').popover({
                html: true,
                placement: "bottom"
            });
            return;
        }

        if (chartData.length == 0) {
            chart = new Dygraph(document.getElementById("google-target-chart"), "X\n", chartOptions);
            return;
        }
        chart = new Dygraph(document.getElementById("google-target-chart"), chartData, chartOptions);
        chart.ready(function () {
        });
    };

    function refresh(start, end, label) {
        var url = "/google/" + $('#csp-vars').attr('data-group-id');
        url += "/target/" + $('#csp-vars').attr('data-target-id');
        url += "?startDate=" + start.format('YYYY-MM-DD');
        url += "&endDate=" + end.format('YYYY-MM-DD');
        url += "&display=" + $('#csp-vars').attr('data-display');
        window.location = url;
    }
    ;

    var stripIntCmp = function (a, b) {
        a = a.replace(/[^0-9]/g, "");
        b = b.replace(/[^0-9]/g, "");
        return a - b;
    };

    var changeCmp = function (a, b) {
        a = a.replace(/\s+/g, "");
        b = b.replace(/\s+/g, "");
        if (a[0] === '-' || a[0] === '+') {
            a = parseInt(a.substring(1), 10);
        }
        if (b[0] === '-' || b[0] === '+') {
            b = parseInt(b.substring(1), 10);
        }

        if (a === "IN" || a === "OUT" || a === "N/A") {
            a = 1000;
        }

        if (b === "IN" || b === "OUT" || b === "N/A") {
            b = 1000;
        }

        return a - b;
    };

    var eventCalendarClick = function () {
        $('#modal-add-event').modal();
        return false;
    };

    var exportSerpsClick = function () {
        $('#modal-export-serps').modal();
        return false;
    };

    var render = function () {
        $(window).bind("load resize", function (evt) {
            resize();
        });
        $('#google-target-table').stupidtable({"stripint": stripIntCmp});
        $('.table-position-change').stupidtable({"change": changeCmp});
        $('input[name="day"]').daterangepicker({
            singleDatePicker: true,
            locale: {
                format: 'YYYY-MM-DD'
            }
        });
        $('#btn-add-event').click(eventCalendarClick);
        $('#modal-export-serps input[name=targetOnly]').change(function(e) {
        	if (e.target.checked) {
            	$('#modal-export-serps input[name=firstTargetOnly]').removeAttr('disabled').parent().show();
        	} else {
            	$('#modal-export-serps input[name=firstTargetOnly]').attr('disabled', 'disabled').parent().hide();
        	}
        });
        $('#btn-export-serps').click(exportSerpsClick);

        if ($('#csp-vars').attr('data-min-date') !== "") {
            $('#daterange').removeAttr("disabled").attr('readonly', true).css('background-color', '#ffffff');
            var maxDate = $('#csp-vars').attr('data-max-date');
            $('#daterange').daterangepicker({
                "ranges": {
                    'Last day': [moment(maxDate), moment(maxDate)],
                    'Last 30 days': [moment(maxDate).subtract(30, 'days'), moment(maxDate)],
                    'Last 90 days': [moment(maxDate).subtract(90, 'days'), moment(maxDate)],
                    'Current Month': [moment(maxDate).startOf('month'), moment(maxDate).endOf('month')],
                    'Previous Month': [moment(maxDate).subtract(1, 'month').startOf('month'), moment(maxDate).subtract(1, 'month').endOf('month')]
                },
                locale: {
                    format: 'YYYY-MM-DD'
                },
                showDropdowns: true,
                startDate: $('#csp-vars').data('display') == "variation" ? 
                    $('#csp-vars').data('end-date') : $('#csp-vars').data('start-date'),
                endDate: $('#csp-vars').attr('data-end-date'),
                minDate: $('#csp-vars').attr('data-min-date'),
                maxDate: $('#csp-vars').attr('data-max-date'),
                singleDatePicker: $('#csp-vars').attr('data-display') == "variation"
            }, refresh);
        }

        serposcope.googleTargetControllerGrid.render();
        serposcope.googleTargetControllerVariation.render();

        var jsonData = JSON.parse($('#csp-vars').attr('data-chart').replace(/NaN/g, '"__NaN__"'), function(key, val) { return val === '__NaN__' ? NaN : val; });
        chartData = [];
        chartStartDate = new Date($('#csp-vars').attr('data-start-date'));
        if (jsonData != null && typeof (jsonData.ranks) != "undefined" && typeof (jsonData.searches) != "undefined") {
            chartData = jsonData.ranks;
            maxRank = jsonData.maxRank > maxRank ? jsonData.maxRank : maxRank;
            var isDraw = jsonData.searches.length < 40 ? true : false;
            for (var i = 0; i < jsonData.searches.length; i++) {
                chartOptions.labels.push(jsonData.searches[i]);
                chartOptions.visibility.push(isDraw);
            }
            chartOptions.valueRange = [maxRank + maxRankOffset, minRank + minRankOffset];
        }
    };

    var oPublic = {
        render: render
    };

    return oPublic;

}();
