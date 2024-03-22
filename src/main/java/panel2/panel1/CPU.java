package panel2.panel1;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashSet;

public class CPU {
    public String[] GPR = new String[4];
    public String[] IXR = new String[3];
    public String PC = "000000000000";
    public String MAR = "000000000000";
    public String MBR = "0000000000000000";
    public String IR = "0000000000000000";
    public String CC = "0000";
    public String MFR = "0000";
    public String Printer="";
    public String Keyboard="";
    public String[] IOregisters = new String[30];
    public int OVERFLOW=0;
    public int DIVZERO=0;
    private HashSet<String> LoadStore;
    private HashSet<String> ArithmeticLogic;
    private HashSet<String> ShiftRotate;
    private HashSet<String> IO;
    public CPU() {

    }

    public void init(){
        Arrays.fill(GPR, "0000000000000000");
        Arrays.fill(IXR, "0000000000000000");
        Arrays.fill(IOregisters, "0000000000000000");
        PC = "000000000000";
        MAR = "000000000000";
        MBR = "0000000000000000";
        IR = "0000000000000000";
        CC = "0000";
        MFR = "0000";
        LoadStore = new HashSet<>();
        LoadStore.add("000001");
        LoadStore.add("000010");
        LoadStore.add("000011");
        LoadStore.add("000100");
        LoadStore.add("000101");
        ArithmeticLogic = new HashSet<>();
        ArithmeticLogic.add("001110");
        ArithmeticLogic.add("001111");
        ArithmeticLogic.add("010000");
        ArithmeticLogic.add("010001");
        ArithmeticLogic.add("010011");
        ArithmeticLogic.add("010100");
        ArithmeticLogic.add("010101");
        ArithmeticLogic.add("010110");
        ArithmeticLogic.add("010111");
        ShiftRotate=new HashSet<>();
        ShiftRotate.add("011000");
        ShiftRotate.add("011001");
        IO = new HashSet<>();
        IO.add("011010");
        IO.add("011011");
    }
    public void setGPR(int index, String content){
        GPR[index]=content;
    }
    public void setIXR(int index, String content){
        IXR[index]=content;
    }
    public void setPC(String content){
        PC=content;
    }
    public void setMAR(String content){
        MAR=content;
    }
    public void setMBR(String content) {
        MBR=content;
    }
    public void setIR(String content) {
        IR=content;
    }
    public void setCC(String content) {
        CC=content;
    }
    public void setMFR(String content) {
        MFR=content;
    }
    public void setConsolePrinter(String content){Printer=content;};
    public void setConsoleKeyboard(String content){Keyboard=content;};
    public String getGPR(int index){
        return GPR[index];
    }
    public String getIXR(int index){
        return IXR[index];
    }
    public String getPC() {
        return PC;
    }
    public String getMAR() {
        return MAR;
    }
    public String getMBR() {
        return MBR;
    }
    public String getIR() {
        return IR;
    }
    public String getCC() {
        return CC;
    }
    public String getMFR() {
        return MFR;
    }
    public String getConsolePrinter(){return Printer;};
    public String getConsoleKeyboard(){return Keyboard;};
    public int step(Memory memory,String input){
        String addressBin = PC;
        int addressDec = Integer.parseInt(addressBin, 2);
        String instructionBin = memory.getInstruction(addressDec);
        if(instructionBin.substring(0,6).equals("000000")){
            memory.setMemoryContent(addressDec, String.format("%16s", instructionBin.substring(6)).replace(' ', '0'));
        }
        int res = parse(instructionBin, memory, input);
        addressDec++;
        String incrementAdd = String.format("%12s",Integer.toBinaryString(addressDec)).replace(' ','0');
        PC=incrementAdd;
        MBR=memory.getInstruction(addressDec);
        MAR=incrementAdd;
        if(addressDec==memory.getEndAddress()){
            PC=(String.format("%16s", Integer.toBinaryString(memory.getFirstAddress())).replace(' ', '0'));
        }
        return 0;
    }
    private int calculateEA(String indexRegisters, String addressingMode, String address, Memory memory){
        int effectiveAddress = 0;
        if (addressingMode.equals("0")) {
            // Direct addressing mode
            if (indexRegisters.equals("00")) {
                effectiveAddress = Integer.parseInt(address,2);
            }
            else {
                // Convert binary strings to integers and calculate the effective address
                int index=Integer.parseInt(indexRegisters,2);
                String contentBin = IXR[index];
                int contentDec = Integer.parseInt(contentBin, 2);
                int addressDec = Integer.parseInt(address, 2);
                int ea = contentDec+addressDec;
                if(ea>memory.getMemorySize()){
                    return -2; // memory index out of range
                }
                effectiveAddress = ea;
            }
        }
        else {
            // Indirect addressing mode
            // for now this does not work (need memory)
            if (indexRegisters.equals("00")) {
                int addressDec = Integer.parseInt(address,2);
                effectiveAddress = Integer.parseInt(memory.getMemoryContent(addressDec), 2);
            }
            else {
                int index=Integer.parseInt(indexRegisters,2);
                String contentBin = IXR[index];
                int contentDec = Integer.parseInt(contentBin, 2);
                int addressDec = Integer.parseInt(address,2);
                int indexedAdd = contentDec+addressDec;
                if(indexedAdd>memory.getMemorySize()){
                    return 4096;// memory index out of range
                }
                effectiveAddress = Integer.parseInt(memory.getMemoryContent(indexedAdd), 2);

            }
        }
        if(effectiveAddress<=5 && effectiveAddress>=0){
            return -1;
        }
        return effectiveAddress;
    }
    private int parse(String instruction, Memory memory, String input){
        // input is instruction's binary form
        if(instruction.length()!=16)
            return 10; // wrong format; reserved for debug
        String opcode = instruction.substring(0,6);
        if(LoadStore.contains(opcode)){
            return handleLoadStore(instruction, memory);
        }
        else if(ArithmeticLogic.contains(opcode)){
            return handleArithmetiLogic(instruction, memory);
        }
        else if(ShiftRotate.contains(opcode)){
            return handleShiftRotate(instruction,memory);
        }
        else if(IO.contains(opcode)){
            return handleIO(instruction,memory,input);
        }
        // easy to develop other instructions under this structure
        return 1;// error;
    }
    private int handleLoadStore(String instruction, Memory memory){
        // Extracting opcode
        String opcode = instruction.substring(0, 6);

        // Extracting general purpose registers
        String generalRegisters = instruction.substring(6, 8);

        // Extracting index registers
        String indexRegisters = instruction.substring(8, 10);

        // Extracting addressing bit
        String addressingMode = instruction.substring(10, 11);

        // Extracting address
        String address = instruction.substring(11);

        // Calculate effective address (EA)
        int EA = calculateEA(indexRegisters, addressingMode, address, memory);
        if(EA<0){
            return -1; // unreachable EA
        }
        int index=0;
        String fromMem="0";
        switch (opcode) {
            case "000001"://LDR r,x,address,[i]
                fromMem = memory.getMemoryContent(EA);
                fromMem=(String.format("%16s", fromMem).replace(' ', '0'));
                index=Integer.parseInt(generalRegisters,2);
                GPR[index]=fromMem;
                return 0;
            case "000010"://STR r,x,address,[i]
                index=Integer.parseInt(generalRegisters,2);
                memory.setMemoryContent(EA, String.format("%16s", GPR[index]).replace(' ', '0'));
                return 0;
            case "000011"://LDA r,x,address,[i]
                index=Integer.parseInt(generalRegisters,2);
                GPR[index]=(String.format("%16s", address).replace(' ', '0'));
                return 0;
            case "000100"://LDX x,address,[i]
                fromMem = memory.getMemoryContent(EA);
                index=Integer.parseInt(indexRegisters,2);
                IXR[index]=(String.format("%16s", fromMem).replace(' ', '0'));
                return 0;
            case "000101"://STX x,address,[i]
                index=Integer.parseInt(indexRegisters,2);
                memory.setMemoryContent(EA, String.format("%16s", IXR[index]).replace(' ', '0'));
                return 0;
            default:
                return 1;
        }
    }
    private int handleArithmetiLogic(String instruction, Memory memory){
        // Extracting opcode
        String opcode = instruction.substring(0, 6);

        // Extracting general purpose registers
        String generalRegisters = instruction.substring(6, 8);

        // Extracting index registers
        String indexRegisters = instruction.substring(8, 10);

        // Extracting addressing bit
        String addressingMode = instruction.substring(10, 11);

        // Extracting address
        String address = instruction.substring(11);
        int EA = calculateEA(indexRegisters, addressingMode, address, memory);
        if(EA<0){
            return -1; // unreachable EA
        }
        int index=0;
        String fromMem="0";
        int cr=0,cea=0,immed=0;
        switch (opcode){
            case "001110":
                //AMR r,x,address,[i]
                fromMem=memory.getMemoryContent(EA);
                index=Integer.parseInt(generalRegisters,2);
                cr=Integer.parseInt(GPR[index],2);
                cea=Integer.parseInt(fromMem,2);
                GPR[index]=String.format("%16s",Integer.toBinaryString(cr+cea)).replace(' ','0');
                return 0;
            case "001111":
                //SMR r,x,address,[i]
                fromMem=memory.getMemoryContent(EA);
                index=Integer.parseInt(generalRegisters,2);
                cr=Integer.parseInt(GPR[index],2);
                cea=Integer.parseInt(fromMem,2);
                GPR[index]=String.format("%16s",Integer.toBinaryString(cr-cea)).replace(' ','0');
                return 0;
            case "010000":
                //AIR r,immed
                index=Integer.parseInt(generalRegisters,2);
                cr=Integer.parseInt(GPR[index],2);
                immed=Integer.parseInt(address.substring(1),2);
                if(address.charAt(0)=='1'){
                    immed=-immed;
                }
                GPR[index]=String.format("%16s",Integer.toBinaryString(cr+immed)).replace(' ','0');
                return 0;
            case "010001":
                //SIR r,immed
                index=Integer.parseInt(generalRegisters,2);
                cr=Integer.parseInt(GPR[index],2);
                immed=Integer.parseInt(address.substring(1),2);
                if(address.charAt(0)=='1'){
                    immed=-immed;
                }
                GPR[index]=String.format("%16s",Integer.toBinaryString(cr-immed)).replace(' ','0');
                return 0;
            case "010010":
                //MLT rx,ry
                String rx=generalRegisters;
                String ry=indexRegisters;
                if((rx.equals("00") || rx.equals("10")) && (ry.equals("00") || ry.equals("10"))){
                    index=Integer.parseInt(rx,2);
                    int operandx=Integer.parseInt(GPR[index],2);
                    int index1=Integer.parseInt(ry,2);
                    int operandy=Integer.parseInt(GPR[index1],2);
                    int multiply=operandx*operandy;
                    String binaryRes=Integer.toBinaryString(multiply);
                    binaryRes=String.format("%32s",binaryRes).replace(' ','0');
                    GPR[index]=binaryRes.substring(0,16);
                    GPR[index1]=binaryRes.substring(16);
                    if(!binaryRes.substring(0,16).equals("0000000000000000")){
                        OVERFLOW=1;
                    }
                }
                else{
                    return 2;//wrong register for multiply
                }
                return 0;
            case "010011":
                //DVD rx,ry
                String rx1=generalRegisters;
                String ry1=indexRegisters;
                if((rx1.equals("00") || rx1.equals("10")) && (ry1.equals("00") || ry1.equals("10"))){
                    index=Integer.parseInt(rx1,2);
                    int operandx=Integer.parseInt(GPR[index],2);
                    int index1=Integer.parseInt(ry1,2);
                    int operandy=Integer.parseInt(GPR[index1],2);
                    if(operandy==0){
                        DIVZERO=1;
                        CC=CC.substring(0,2)+'1'+CC.substring(3);
                    }
                    int quotient=operandx/operandy;
                    int remainder=operandx%operandy;
                    String binaryQ=Integer.toBinaryString(quotient);
                    String binaryR=Integer.toBinaryString(remainder);
                    binaryQ=String.format("%16s",binaryQ).replace(' ','0');
                    binaryR=String.format("%16s",binaryR).replace(' ','0');
                    GPR[index]=binaryQ;
                    GPR[index1]=binaryR;
                }
                else{
                    return 2;//wrong register for multiply
                }
                return 0;
            case "010100":
                //TRR rx,ry
                index=Integer.parseInt(generalRegisters,2);
                int indexy=Integer.parseInt(indexRegisters,2);
                int rx2=Integer.parseInt(GPR[index],2);
                int ry2=Integer.parseInt(GPR[indexy],2);
                if(rx2==ry2){
                    CC=CC.substring(0,3)+"1";
                }
                else{
                    CC=CC.substring(0,3)+"0";
                }
                return 0;
            case "010101":
                //AND rx,ry
                index=Integer.parseInt(generalRegisters,2);
                int index1=Integer.parseInt(indexRegisters,2);
                int rx3=Integer.parseInt(GPR[index],2);
                int ry3=Integer.parseInt(GPR[index1],2);
                rx3=rx3&ry3;
                GPR[index]=String.format("%16s",Integer.toBinaryString(rx3)).replace(' ','0');
                return 0;
            case "010110":
                //ORR rx,ry
                index=Integer.parseInt(generalRegisters,2);
                int index2=Integer.parseInt(indexRegisters,2);
                int rx4=Integer.parseInt(GPR[index],2);
                int ry4=Integer.parseInt(GPR[index2],2);
                rx4=rx4|ry4;
                GPR[index]=String.format("%16s",Integer.toBinaryString(rx4)).replace(' ','0');
                return 0;
            case "010111":
                //NOT rx
                index=Integer.parseInt(generalRegisters,2);
                int rx5=Integer.parseInt(GPR[index],2);
                int nrx=~rx5;
                GPR[index]=String.format("%16s",Integer.toBinaryString(nrx)).replace(' ','0');
                return 0;
            default:
                return 1;
        }
    }
    private int handleShiftRotate(String instruction, Memory memory){
        // Extracting opcode
        String opcode = instruction.substring(0, 6);

        // Extracting general purpose registers
        String generalRegisters = instruction.substring(6, 8);

        // Extracting LR\AL
        String LR = instruction.substring(8, 9);
        String AL = instruction.substring(9,10);

        // Extracting address
        String count = instruction.substring(11);
        int index=0,cnt=0;
        switch (opcode){
            case "011000":
                //SRC r,count,L/R,A/L
                cnt=Integer.parseInt(count,2);
                index=Integer.parseInt(generalRegisters,2);
                String content=GPR[index];
                StringBuilder result=new StringBuilder(content);
                if(cnt==0 || cnt==16){
                    return 0;
                }
                if(LR.equals("1")){//left shift
                    for (int i = 0; i < cnt; i++) {
                        result.deleteCharAt(0);      // Remove the leftmost bit.
                        result.append("0");          // Append a zero bit to the right end.
                    }
                    GPR[index]=String.format("%16s",result.toString()).replace(' ','0');
                    return 0;
                }
                else{
                    //right shift
                    if (AL.equals("1")) { // Logical shift right
                        for (int i = 0; i < cnt; i++) {
                            result.deleteCharAt(result.length() - 1); // Remove the rightmost bit.
                            result.insert(0, "0");                    // Insert a zero bit to the left end.
                        }
                    } else { // Arithmetic shift right
                        char signBit = content.charAt(0); // Store the original sign bit.
                        for (int i = 0; i < cnt; i++) {
                            result.deleteCharAt(result.length() - 1); // Remove the rightmost bit.
                            result.insert(0, Character.toString(signBit)); // Insert the sign bit to the left end.
                        }
                    }
                    GPR[index]=String.format("%16s",result.toString()).replace(' ','0');
                }
                return 0;
            case "011001":
                //RRC r,count,L/R,A/L
                cnt=Integer.parseInt(count,2);
                index=Integer.parseInt(generalRegisters,2);
                String content1=GPR[index];
                if(cnt==0 || cnt==16){
                    return 0;
                }
                for (int i = 0; i < cnt; i++) {
                    if (LR.equals("1")) { // Rotate left
                        // Move the first character to the end
                        content1 = content1.substring(1) + content1.charAt(0);
                    } else { // Rotate right
                        // Move the last character to the beginning
                        content1 = content1.charAt(content1.length() - 1) + content1.substring(0, content1.length() - 1);
                    }
                }
                return 0;
            default:
                return 1;
        }
    }
    private int handleIO(String instruction, Memory memory, String input){
        // Extracting opcode
        String opcode = instruction.substring(0, 6);

        // Extracting general purpose registers
        String generalRegisters = instruction.substring(6, 8);

        // Extracting address
        String devID = instruction.substring(11);
        int index=0;
        int devid=Integer.parseInt(devID,2);
        switch (opcode){
            case "011010":
                //IN r,devid
                index=Integer.parseInt(generalRegisters,2);
                if(devid==0 || devid==1 || devid==2){
                    switch (devid){
                        case 0:
                            //Keyboard
                            GPR[index]=String.format("%16s",input).replace(' ','0');
                            return 0;
                        case 2:
                            //card reader
                        default:
                            return 1;//wrong dev
                    }
                }
                else{
                   GPR[index]=IOregisters[devid];
                }
            case "011011":
                //OUT r,devid
                index=Integer.parseInt(generalRegisters,2);
                if(devid==0 || devid==1 || devid==2){
                    if(devid==1){
                        //printer
                        StringBuilder sb=new StringBuilder(Printer);
                        sb.append(GPR[index]);
                        Printer=sb.toString();
                        return 0;
                    }
                }
                else{
                    IOregisters[devid]=String.format("%16s",input).replace(' ','0');
                }
            default:
                return 1;
        }
    }
}