package com.adaptiq.service;

import com.adaptiq.dao.RecommendationDAO;
import com.adaptiq.exception.AuthorizationException;
import com.adaptiq.exception.ReportException;
import com.adaptiq.model.Recommendation;
import com.adaptiq.model.Role;
import com.adaptiq.model.User;
import com.adaptiq.model.dashboard.FacultyDashboardData;
import com.adaptiq.util.SessionManager;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Generates selected local PDF files from live service data. */
public final class PdfReportService {
    public void generateStudentReport(Path destination) {
        User user=role(Role.STUDENT); AnalyticsSnapshot analytics=new AnalyticsService().myAnalytics();
        java.util.List<Recommendation> recommendations=new RecommendationDAO().findActive(user.getUserId());
        write(destination,"AdaptIQ Student Performance Report",document->{
            add(document,"Student: "+user.getFullName()+" ("+user.getEmail()+")",false);
            add(document,"Tests attempted: "+analytics.testsAttempted(),false); add(document,"Average score: "+analytics.averageScore(),false); add(document,"Highest score: "+analytics.highestScore(),false); add(document,"Overall accuracy: "+analytics.overallAccuracy()+"%",false);
            add(document,"Topic-wise performance",true); analytics.topicAccuracy().forEach(item->add(document,item.name()+": "+item.accuracy()+"%",false));
            add(document,"Recommended practice",true); if(recommendations.isEmpty()) add(document,"No active recommendations.",false); else recommendations.forEach(item->add(document,item.getTopicName()+": "+item.getRecommendationText(),false));
        });
    }
    public void generateFacultyReport(Path destination) {
        User user=role(Role.FACULTY); FacultyDashboardData data=new DashboardService().facultyDashboard();
        write(destination,"AdaptIQ Faculty Examination Report",document->{add(document,"Faculty: "+user.getFullName(),false);add(document,"Total examinations: "+data.totalExams(),false);add(document,"Published examinations: "+data.publishedExams(),false);add(document,"Student attempts: "+data.totalAttempts(),false);add(document,"Average result: "+data.averagePercentage()+"%",false);});
    }
    private void write(Path destination,String title,Writer writer) {
        try(OutputStream stream=Files.newOutputStream(destination)){Document document=new Document();PdfWriter.getInstance(document,stream);document.open();add(document,title,true);writer.write(document);document.close();}catch(Exception e){throw new ReportException("Could not generate the PDF report.",e);}
    }
    private void add(Document document,String text,boolean heading){try{document.add(new Paragraph(text,FontFactory.getFont(FontFactory.HELVETICA,heading?16:11,heading?Font.BOLD:Font.NORMAL)));}catch(Exception e){throw new ReportException("Could not write the PDF report.",e);}}
    private User role(Role expected){User user=SessionManager.currentUser().orElseThrow(()->new AuthorizationException("Please sign in to continue."));if(user.getRole()!=expected)throw new AuthorizationException("You are not authorized to generate this report.");return user;}
    @FunctionalInterface private interface Writer { void write(Document document); }
}
