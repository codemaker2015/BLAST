package mage.Tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import mage.Core.Oligo;
import mage.Core.Primer;
import mage.Tools.Pcr.Melt;
import mage.Tools.Pcr.PCR;

/**
 * Class to handle writing to files. This is the entry point for software (eg
 * Magelet) using the Tools package to generate outputs via the DSDNA,
 * OligoStats, and PCR classes
 *
 * @author Michael Quintin
 *
 */
public class OutputTools {

    /**
     * Generate the MASC PCR primers from the given oligos, and output them to
     * the given file This is the function called by the servlet
     *
     * @param pool
     * @param dest
     * @throws IOException
     */
    public static void generateMASCPCRFile(List<Oligo> pool, String dest) throws IOException {
        File f = new File(dest);
        if (!f.isFile()) { //only bother if the file hasn't been made
            PCR pcr = new PCR();
            ArrayList<ArrayList<Primer>> primers = pcr.generateAllPrimers(pool);
            writePrimersToFile(primers, dest);
        }
    }

    /**
     * get the file contents for the MASCPCR file as a string, to be handled
     * upstream
     *
     * @param pool
     * @return
     * @throws IOException
     */
    /*TODO: decomment
     public static String getMASCPCRPrimerFileContents(List<Oligo> pool) throws IOException{
     List<List<String>> primers = PCR.getMASCPCRPrimers(pool);
     List<String> names = new ArrayList<String>();
     for (Oligo oligo : pool){
     names.add(oligo.name);
     }
     String header = ("Oligo\tUnmodifiedForward\tModifiedForward");
     for(int length : PCR.getAmpliconLengths()){
     header = header + "\t" + "Rev" + String.valueOf(length);
     }
     String contents = header + System.getProperty("line.separator");
     for (int i = 0; i < primers.size(); i++){
     String str = names.get(i);
     for (String primer : primers.get(i)){
     str = str + "\t" + primer;
     }
     contents = contents + str + System.getProperty("line.separator");
     }
     return(contents);
     }*/
    /**
     * Given the primer sets generated by PCR.generateAllPrimers(), format into
     * table form and output to the default file location
     *
     * @param lol "list of lists" An array of arrays of primers. The first
     * member is the set of forward primers
     * @param dest file to create
     */
    public static void writePrimersToFile(ArrayList<ArrayList<Primer>> lol, String dest) throws IOException {
        //pull out the forward primers
        ArrayList<Primer> fwd = lol.get(0);
        HashMap<Oligo, Primer> base = new HashMap<>(); //unmodified forward primers
        HashMap<Oligo, Primer> mod = new HashMap<>(); //modified forward primers
        for (Primer p : fwd) {
            if (p.modified) {
                mod.put(p.oligo, p);
            } else {
                base.put(p.oligo, p);
            }
        }

        File file = new File(dest);

        // if file doesn't exists, then create it
        if (!file.exists()) {
            file.createNewFile();
        }

        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);

        //generate the header
        bw.write("Target Temperature: " + PCR.targetTemp + "\t");
        bw.newLine();
        String cmdArgs = Melt.getCmdArgs().trim();
        cmdArgs = cmdArgs.replaceAll(" ", "\t");
        cmdArgs = cmdArgs.replaceAll("=", ": ");
        cmdArgs = cmdArgs.replace("dnac1", "Primer(nM)");
        cmdArgs = cmdArgs.replace("dnac2", "TemplateDNA(nM)");
        bw.write(cmdArgs);
        bw.newLine();
        bw.newLine();
        bw.write("Set\tAmpLen\tTarget\tFwdBase\tMT\tFwdMod\tMT\tRev\tMT");
        //iterate the primer sets
        for (int i = 1; i < lol.size(); i++) {
            ArrayList<Primer> list = lol.get(i);
            for (Primer p : list) {
                Oligo o = p.oligo;
                Primer bf = base.get(o);
                Primer mf = mod.get(o);
                bw.newLine();
                bw.write(getPrimerOutput(i, bf, mf, p));
            }
        }
        bw.close();
    }

    /**
     * Get the line representing this group of primers for the output table
     * matching the header "Set AmpLen Target FwdBase MT FwdMod MT Rev MT"
     *
     * @param set which set of primers will this go into?
     * @param base unmodified forward primer
     * @param mod modified forward primer
     * @param rev reverse primer
     */
    private static String getPrimerOutput(int set, Primer base, Primer mod, Primer rev) {
        String s = "";
        s = s.concat(String.valueOf(set));
        s = s.concat("\t");
        s = s.concat(String.valueOf(rev.amplicon));
        s = s.concat("\t");
        s = s.concat(rev.oligo.name);
        s = s.concat("\t");
        s = s.concat(base.seq);
        s = s.concat("\t");
        s = s.concat(String.valueOf(base.getMt()));
        s = s.concat("\t");
        s = s.concat(mod.seq);
        s = s.concat("\t");
        s = s.concat(String.valueOf(mod.getMt()));
        s = s.concat("\t");
        s = s.concat(rev.seq);
        s = s.concat("\t");
        s = s.concat(String.valueOf(rev.getMt()));

        return s;
    }

    /**
     * Generate a file containing the two DSDNA primers for the given
     * recombination. Note that the start and end are indexed with 1 (not 0)
     * being the first base on the genome
     *
     * An error message will be generated instead if the size of the replacement
     * is too small (not at least twice the size of the overlap into the
     * sequence to be inserted)
     *
     * @param genome
     * @param sequence Sequence of the insertion
     * @param leftpos 1-indexed base position on the genome to begin the
     * replacement
     * @param rightpos 1-indexed base position on the genome to end the
     * replacement
     * @param dest
     * @throws IOException
     */
    public static void generateDSDNAPrimerFile(String genome, String sequence, int leftpos, int rightpos, String dest) throws IOException {
        //make sure the start and end are in the right order
        int start = leftpos;
        int end = rightpos;
        if (leftpos > rightpos) {
            start = rightpos;
            end = leftpos;
        }

        List<String> primers = DSDNA.getDSDNAPrimers(genome, sequence, start, end);
        writeDSDNAPrimersToFile(primers, dest);
    }

    /**
     * Generate the two DSDNA primers for the given recombination. Note that the
     * start and end are indexed with 1 (not 0) being the first base on the
     * genome
     *
     * An error message will be generated instead if the size of the replacement
     * is too small (not at least twice the size of the overlap into the
     * sequence to be inserted)
     *
     * @param genome
     * @param sequence Sequence of the insertion
     * @param leftpos 1-indexed base position on the genome to begin the
     * replacement
     * @param rightpos 1-indexed base position on the genome to end the
     * replacement
     */
    public static List<String> getDSDNAPrimers(String genome, String sequence, int leftpos, int rightpos) {
        //make sure the start and end are in the right order
        int start = leftpos;
        int end = rightpos;
        if (leftpos > rightpos) {
            start = rightpos;
            end = leftpos;
        }

        return DSDNA.getDSDNAPrimers(genome, sequence, start, end);
    }

    /**
     * Write the DSDNA primers to the given location
     *
     * @param primers
     * @param dest
     * @throws IOException
     */
    public static void writeDSDNAPrimersToFile(List<String> primers, String dest) throws IOException {
        //if there are two elements in the primers list, it worked. otherwise it's an error message

        File file = new File(dest);

        // if file doesn't exists, then create it
        if (!file.exists()) {
            file.createNewFile();
        }

        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);

        for (String str : primers) {
            bw.write(str);
            bw.newLine();
        }
        bw.close();
    }

    public static void generateDiversityTrendTableFile(List<Oligo> oligos, int cycles, String dest) throws IOException {
        //String table = OligoStats.getDiversityTable(oligos, cycles);
        String table = "";
        List<List<Double>> olist = OligoStats.getDiscreteDiversityTable(oligos, cycles);
        //System.out.println(table);
        for (List<Double> list : olist) {
            for (Double d : list) {
                table = table.concat(d.toString()) + "\t";
            }
            table = table.trim().concat(System.getProperty("line.separator"));
        }
        writeDiversityTableToFile(table, dest);
    }

    public static void writeDiversityTableToFile(String table, String dest) throws IOException {
        File file = new File(dest);

        // if file doesn't exists, then create it
        if (!file.exists()) {
            file.createNewFile();
        }

        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        //table is already formatted
        bw.write(table);
        bw.close();
    }

    public static void generateAREFile(List<Oligo> oligos, String dest) throws IOException {
        String table = "";
        for (Oligo oligo : oligos) {
            table.concat(oligo.name + ": " + OligoStats.getARE(oligo) + "\n");
            //System.err.println(oligo.name + ": " + OligoStats.getARE(oligo));
        }
        //table.concat("getAggregateAnyARE: " + OligoStats.getAggregateAnyARE(oligos)+"\n");
        //table.concat("getAggregateSumARE: " + OligoStats.getAggregateSumARE(oligos)+"\n");
        //System.err.println("getAggregateAnyARE: " + OligoStats.getAggregateAnyARE(oligos));
        //System.err.println("getAggregateSumARE: " + OligoStats.getAggregateSumARE(oligos)+"\n");
        writeARETableToFile(table, dest);
    }

    public static void writeARETableToFile(String table, String dest) throws IOException {
        File file = new File(dest);

        // if file doesn't exists, then create it
        if (!file.exists()) {
            file.createNewFile();
        }

        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        //table is already formatted
        bw.write(table);
        bw.close();
    }
}
