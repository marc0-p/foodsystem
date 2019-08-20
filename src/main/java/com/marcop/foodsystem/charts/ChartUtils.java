package com.marcop.foodsystem.charts;

import com.google.common.base.Joiner;
import com.google.common.collect.TreeMultimap;
import com.marcop.foodsystem.model.Order;
import com.marcop.foodsystem.model.OrderState;
import org.apache.hadoop.fs.Path;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Map;

import static org.jfree.chart.ChartUtils.saveChartAsPNG;

public class ChartUtils {

    // File name constants
    private static final String ordersByPriceFileName = "charts/orders_by_price.png";
    private static final String ordersByPendingTimeFileName = "charts/orders_by_pending_time.png";
    private static final String revenueByServiceFileName = "charts/revenue_by_service.png";
    private static final String revenueByItemFileName = "csv/revenue_by_item.csv";
    private static final String orderStatesOverTimeFileName = "charts/order_states_over_time.png";

    private static final NumberFormat currencyFormatterDollars;
    static
    {
        currencyFormatterDollars = NumberFormat.getCurrencyInstance();
        currencyFormatterDollars.setMinimumFractionDigits(0);
    }

    private static final NumberFormat currencyFormatterDollarsAndCents;
    static
    {
        currencyFormatterDollarsAndCents = NumberFormat.getCurrencyInstance();
    }

    public static final String STATS_PAGE_FILE_NAME = "index.html";

    /** Create chart (PNG) for Orders by price. */
    private static void createOrderByPriceChart(TreeMultimap<Integer, Order> ordersByPrice,
                                               Path outputPath) throws IOException {
        XYSeries series = new XYSeries("Order Price");
        XYSeriesCollection dataset = new XYSeriesCollection();
        int orderNumber = 1;
        for (Map.Entry entry : ordersByPrice.entries()) {
            series.add(orderNumber, ((double) ((Integer) entry.getKey())) / 100.0);
            orderNumber++;
        }
        dataset.addSeries(series);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Orders by Total Price",
                "Orders",
                "Order Price ($)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
        outputPath = new Path(outputPath, ordersByPriceFileName);
        File outFile = new File(outputPath.toString());
        outFile.getParentFile().mkdirs();
        outFile.createNewFile();
        saveChartAsPNG(outFile, chart, 640, 480);
    }

    /** Create chart (PNG) for Orders by pending duration. */
    private static void createOrderByPendingTime(TreeMultimap<Integer, Order> ordersByPendingTime,
                                                Path outputPath) throws IOException {
        XYSeries series = new XYSeries("Time Order is in Pending State (minutes)");
        XYSeriesCollection dataset = new XYSeriesCollection();
        int orderNumber = 1;
        for (Map.Entry entry : ordersByPendingTime.entries()) {
            series.add(orderNumber, ((Integer) entry.getKey()));
            orderNumber++;
        }
        dataset.addSeries(series);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Time Order is in Pending State (minutes)",
                "Orders",
                "Pending State Duration (minutes)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
        outputPath = new Path(outputPath, ordersByPendingTimeFileName);
        File outFile = new File(outputPath.toString());
        outFile.getParentFile().mkdirs();
        outFile.createNewFile();
        saveChartAsPNG(outFile, chart, 640, 480);
    }

    /** Create chart (PNG) for Revenue by Item. */
    private static void createRevenueByServiceChart(Map<String, Integer> revenueByService, Path outputPath)
            throws IOException {
        DefaultPieDataset dataset = new DefaultPieDataset( );
        for (String service : revenueByService.keySet()) {
            dataset.setValue(service, revenueByService.get(service) / 100);
        }

        JFreeChart chart = ChartFactory.createPieChart(
                "Revenue by Service",
                dataset,
                true,
                true,
                false);
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setSimpleLabels(true);
        PieSectionLabelGenerator gen = new StandardPieSectionLabelGenerator(
                "{0}\n{1} ({2})", currencyFormatterDollars, new DecimalFormat("0%"));
        plot.setLabelGenerator(gen);

        outputPath = new Path(outputPath, revenueByServiceFileName);
        File outFile = new File(outputPath.toString());
        outFile.getParentFile().mkdirs();
        outFile.createNewFile();
        saveChartAsPNG(outFile, chart, 640, 480);
    }

    /** Create Table for Revenue by Item. */
    private static String revenueByItemTable(Map<String, Integer> revenueByItem, Path outputPath) throws IOException {
        StringBuilder stringCsv = new StringBuilder();
        StringBuilder stringMapTable = new StringBuilder();

        stringMapTable.append("<table>");

        Iterator it = revenueByItem.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            String item = (String) pair.getKey();
            double revenue = (double) ((Integer) pair.getValue()) / 100;
            stringMapTable.append("<tr><td>" + item + "</td><td>"
                    + currencyFormatterDollarsAndCents.format(revenue)
                    + "</td></tr>");
            stringCsv.append(Joiner.on(',').join(item, revenue, "\n"));
            it.remove(); // avoids a ConcurrentModificationException
        }
        // Save raw CSV and return formatted table for display.
        File file = new File(new Path(outputPath, revenueByItemFileName).toString());
        file.getParentFile().mkdirs();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(stringCsv.toString());
        }
        return stringMapTable.toString();
    }

    /** Create chart (PNG) for Order states over time. */
    private static void createOrderStateCountsByTimeChart(
            Map<Timestamp, Map<OrderState, Integer>> orderStateCountsByTime, Path outputPath) throws IOException {
        TimeSeriesCollection dataset = new TimeSeriesCollection();

        TimeSeries pendingSeries = new TimeSeries("Number of Orders Pending");
        TimeSeries processingSeries = new TimeSeries("Number of Orders in Processing");
        TimeSeries completedSeries = new TimeSeries("Number of Orders Completed");

        for (Timestamp timestamp : orderStateCountsByTime.keySet()) {
            Map<OrderState, Integer> orderStateCounts = orderStateCountsByTime.get(timestamp);

            try {
                if (orderStateCounts.containsKey(OrderState.CREATED)) {
                    pendingSeries.add(new Millisecond(timestamp), orderStateCounts.get(OrderState.CREATED));
                } else {
                    pendingSeries.add(new Millisecond(timestamp), 0);
                }

                if (orderStateCounts.containsKey(OrderState.PROCESSING)) {
                    processingSeries.add(new Millisecond(timestamp), orderStateCounts.get(OrderState.PROCESSING));
                } else {
                    processingSeries.add(new Millisecond(timestamp), 0);
                }

                if (orderStateCounts.containsKey(OrderState.COMPLETE)) {
                    completedSeries.add(new Millisecond(timestamp), orderStateCounts.get(OrderState.COMPLETE));
                } else {
                    completedSeries.add(new Millisecond(timestamp), 0);
                }
            } catch (Exception e) {
            }
        }

        dataset.addSeries(pendingSeries);
        dataset.addSeries(processingSeries);
        dataset.addSeries(completedSeries);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Order States Over Time", "Time", "Order State Counts", dataset, true, true, false);
        XYPlot plot = (XYPlot) chart.getPlot();
        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("dd-MMM HH:mm"));
        axis.setTickUnit(new DateTickUnit(DateTickUnitType.HOUR, 4));
        axis.setVerticalTickLabels(true);
        outputPath = new Path(outputPath, orderStatesOverTimeFileName);
        File outFile = new File(outputPath.toString());
        outFile.getParentFile().mkdirs();
        outFile.createNewFile();
        saveChartAsPNG(outFile, chart, 1920, 480);
    }

    public static void createStatsPage(
            String kitchenName,
            int maxConcurrentItems,
            TreeMultimap<Integer, Order> ordersByPrice,
            TreeMultimap<Integer, Order> ordersByPendingTime,
            Map<Timestamp, Map<OrderState, Integer>> orderStateCountsByTime,
            Map<String, Integer> revenueByItem,
            Map<String, Integer> revenueByService,
            int totalRevenue,
            int rejectedOrderCount,
            Path outputPath) throws IOException {
        createOrderByPriceChart(ordersByPrice, outputPath);
        createOrderByPendingTime(ordersByPendingTime, outputPath);
        createOrderStateCountsByTimeChart(orderStateCountsByTime, outputPath);
        createRevenueByServiceChart(revenueByService, outputPath);

        try {

            OutputStream htmlfile= new FileOutputStream(
                    new File(new Path(outputPath, STATS_PAGE_FILE_NAME).toString()));
            PrintStream printhtml = new PrintStream(htmlfile);

            String htmlTxt = "";
            htmlTxt += "<html><head>";
            htmlTxt += "<title>Food System Stats</title>";
            htmlTxt += "</head><body>";
            htmlTxt += "<h1>Food System Stats</h1>\n";

            // Kitchen Info
            htmlTxt += "<h2>Kitchen Information</h2>\n";
            htmlTxt += "<p>Kitchen name: " + kitchenName + "</p>\n";
            htmlTxt += "<p>Maximum allowed concurrent item processing: " +
                    (maxConcurrentItems == 0 ? "INF" : maxConcurrentItems) + "</p>\n";

            // Descriptive stats
            htmlTxt += "<h2>Descriptive Stats</h2>\n";
            htmlTxt += "<p>Total orders received: " + (rejectedOrderCount + ordersByPrice.size()) + "</p>\n";
            htmlTxt += "<p>Orders successfully processed: " + ordersByPrice.size() + "</p>\n";
            htmlTxt += "<p>Orders rejected (typically due to having zero items): " + rejectedOrderCount + "</p>\n";
            htmlTxt += "<p>Total revenue: " + currencyFormatterDollars.format((totalRevenue) / 100) + "</p>\n";

            // Charts
            htmlTxt += "<h2>Charts</h2>";
            htmlTxt+="<p><img src=\"" + ordersByPriceFileName + "\"></p>";
            htmlTxt+="<p><img src=\"" + ordersByPendingTimeFileName + "\"></p>";
            htmlTxt+="<p><img src=\"" + revenueByServiceFileName + "\"></p>";
            htmlTxt+="<p><img src=\"" + orderStatesOverTimeFileName + "\"></p>";

            // Tables
            htmlTxt += "<h2>Tables</h2>";
            htmlTxt += "<h3>Revenue by Item (also available as raw CSV in output DIR)</h3>";
            htmlTxt+="<p>" + revenueByItemTable(revenueByItem, outputPath) + "</p>";

            htmlTxt+="</body></html>";
            printhtml.println(htmlTxt);
            printhtml.close();
            htmlfile.close();
        }

        catch (Exception e) {}
    }
}
