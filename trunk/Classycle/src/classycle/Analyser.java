/*
 * Copyright (c) 2003-2004, Franz-Josef Elmer, All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, 
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, 
 *   this list of conditions and the following disclaimer in the documentation 
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */
package classycle;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map;

import classycle.graph.AtomicVertex;
import classycle.graph.StrongComponent;
import classycle.graph.StrongComponentAnalyser;
import classycle.renderer.AtomicVertexRenderer;
import classycle.renderer.PlainStrongComponentRenderer;
import classycle.renderer.StrongComponentRenderer;
import classycle.renderer.TemplateBasedClassRenderer;
import classycle.renderer.XMLClassRenderer;
import classycle.renderer.XMLStrongComponentRenderer;
import classycle.util.StringPattern;
import classycle.util.Text;
import classycle.util.TrueStringPattern;

/**
 * Main class of the Classycle tool. Runs on the command line and
 * produces a report.
 * 
 * @author Franz-Josef Elmer
 */
public class Analyser 
{
  private static final String VERSION = "1.0";
  private static final String CSV_TEMPLATE 
                                      = "{0},{1},{3},{2},{4},{5},{6},{7}\n";
  
  private final String[] _classFiles;
  private final StringPattern _pattern;
  private StrongComponentAnalyser _classAnalyser;
  private StrongComponentAnalyser _packageAnalyser;
  
  /**
   * Creates an instance for the specified files or folders.
   * @param classFiles Absolute or relative file names.
   */
  public Analyser(String[] classFiles) 
  {
    this(classFiles, new TrueStringPattern());
  }

  /**
   * Creates an instance for the specified files or folders which are
   * filtered by the specified {@link StringPattern} object.
   * @param classFiles Absolute or relative file names.
   * @param pattern Pattern fully-qualified class name have to match in order
   *        to be a part of the class graph.
   */
  public Analyser(String[] classFiles, StringPattern pattern) 
  {
    _classFiles = classFiles;
    _pattern = pattern;
  }
  
  /**
   * Parses the class files and creates the class graph.
   * @return the duration of this operation in milliseconds.
   * @throws IOException if a problem occured during reading
   */
  public long createClassGraph() throws IOException 
  {
    long time = System.currentTimeMillis();
    _classAnalyser = new StrongComponentAnalyser(
                            Parser.readClassFiles(_classFiles, _pattern));
    return System.currentTimeMillis() - time;
  }
  
  /**
   * Returns the class graph. Invokes {@link #createClassGraph()} if not
   * already invoked.
   */
  public AtomicVertex[] getClassGraph() 
  {
    if (_classAnalyser == null)
    {
      try
      {
        createClassGraph();
      } catch (IOException e)
      {
        throw new RuntimeException(e.toString());
      }
    }
    return _classAnalyser.getGraph();
  }
  
  /**
   * Counts the number of external classes.
   */
  public int getNumberOfExternalClasses()
  {
    AtomicVertex[] graph = getClassGraph();
    HashSet usedClasses = new HashSet();
    int result = 0;
    for (int i = 0; i < graph.length; i++) 
    {
      AtomicVertex vertex = graph[i];
      for (int j = 0, n = vertex.getNumberOfOutgoingArcs(); j < n; j++) 
      {
        ClassAttributes attributes =
            (ClassAttributes) vertex.getHeadVertex(j).getAttributes();
        if (attributes.getType() == ClassAttributes.UNKNOWN) 
        {
          if (!usedClasses.contains(attributes.getName())) 
          {
            result++;
            usedClasses.add(attributes.getName());
          }
        }
      }
    }
    return result;
  }

  /**
   * Condenses the class graph to an acyclic graph of its strong components.
   * @return the duration of this operation in milliseconds.
   * @throws IllegalStateException if this method is called before
   *         {@link #createClassGraph()}.
   */
  public long condenseClassGraph() 
  {
    checkClassGraph("condenseClassGraph()");
    long time = System.currentTimeMillis();
    _classAnalyser.getCondensedGraph();
    return System.currentTimeMillis() - time;
  }

  /**
   * Returns the condensed the class graph, i.e.&nbsp;the acyclic graph of 
   * its strong components.
   * @throws IllegalStateException if this method is called before
   *         {@link #createClassGraph()}.
   */
  public StrongComponent[] getCondensedClassGraph() 
  {
    checkClassGraph("getCondenseClassGraph()");
    return _classAnalyser.getCondensedGraph();
  }

  /**
   * Calculates the for each class its layer index. The layer index of a
   * class is the length of the longest path in the acyclic graph of strong 
   * components starting at the strong component to which the class belongs. 
   * @return the duration of this operation in milliseconds.
   * @throws IllegalStateException if this method is called before
   *         {@link #createClassGraph()}.
   */
  public long calculateClassLayerMap()
  {
    checkClassGraph("calculateClassLayerMap()");
    long time = System.currentTimeMillis();
    _classAnalyser.getLayerMap();
    return System.currentTimeMillis() - time;
  }

  /**
   * Calculates the for each class its layer index and returns
   * a <tt>Map</tt> where the classes are the keys (type {@link AtomicVertex})
   * and the layer indices are the values (type <tt>Integer</tt>).
   * @throws IllegalStateException if this method is called before
   *         {@link #createClassGraph()}.
   */
  public Map getClassLayerMap()
  {
    checkClassGraph("getClassLayerMap()");
    return _classAnalyser.getLayerMap();
  }

  /**
   * Creates the package graph from the class graph.
   * @return the duration of this operation in milliseconds.
   * @throws IllegalStateException if this method is called before
   *         {@link #createClassGraph()}.
   */
  public long createPackageGraph() 
  {
    checkClassGraph("createPackageGraph()");
    long time = System.currentTimeMillis();
    PackageProcessor processor = new PackageProcessor();
    processor.deepSearchFirst(_classAnalyser.getGraph());
    _packageAnalyser = new StrongComponentAnalyser(processor.getGraph());
    return System.currentTimeMillis() - time;
  }
  
  /**
   * Prints a CSV report into the specified writer. Delimiter is ','.
   * First, a header with column titles is print. The columns are
   * <ol><li>class name
   *     <li>inner class (<tt>false</tt> or <tt>true</tt>)
   *     <li>size (in bytes)
   *     <li>used by (number of classes using this class)
   *     <li>uses internal classes 
   *         (number of classes of the graph used by this class)
   *     <li>uses external classes 
   *         (number of external classes used by this class)
   *     <li>layer index  
   * </ol>
   * @param writer Output stream.
   */
  public void printCSV(PrintWriter writer) 
  {
    AtomicVertex[] graph = getClassGraph();
    Map map = getClassLayerMap();
    writer.println("class name,type,inner class,size,used by,"
            + "uses internal classes,uses external classes,layer index");
    render(graph, map, new TemplateBasedClassRenderer(CSV_TEMPLATE), writer);
    writer.close();
  }

  /**
   * Prints for each class a raw output into the specified writer.
   * This output includes all classes used by the class.
   * @param writer Output stream.
   */
  public void printRaw(PrintWriter writer)
  {
    AtomicVertex[] graph = getClassGraph();
    for (int i = 0; i < graph.length; i++) 
    {
      AtomicVertex vertex = graph[i];
      writer.println(vertex.getAttributes());
      for (int j = 0, n = vertex.getNumberOfOutgoingArcs(); j < n; j++) 
      {
        writer.println("    " + vertex.getHeadVertex(j).getAttributes());
      }
    }
    writer.close();
  }

  /**
   * Prints for each strong component of the class graph a raw output into 
   * the specified writer. The strong component must have at least 
   * <tt>minSize</tt> classes in order to be printed out. 
   * This output includes all classes of the strong component.
   * @param writer Output stream.
   * @param minSize Minimum size of the strong component.
   * @throws IllegalStateException if this method is called before
   *         {@link #createClassGraph()}.
   */
  public void printComponents(PrintWriter writer, int minSize)
  {
    checkClassGraph("printComponents()");
    StrongComponent[] components = getCondensedClassGraph();
    StrongComponentRenderer renderer = new PlainStrongComponentRenderer();
    for (int i = 0; i < components.length; i++) 
    {
      StrongComponent component = components[i];
      if (component.getNumberOfVertices() >= minSize) 
      {
        writer.println(renderer.render(component));
      }
    }
    writer.close();
  }

  private void checkClassGraph(String method)
  {
    if (_classAnalyser == null)
    {
      throw new IllegalStateException(
                  method + " should be invoked after createClassGraph().");
    }
  }

  /**
   * Returns the package graph created the class graph.
   * @throws IllegalStateException if this method is called before
   *         {@link #createClassGraph()}.
   */
  public AtomicVertex[] getPackageGraph() 
  {
    if (_packageAnalyser == null)
    {
      createPackageGraph();
    }
    return _packageAnalyser.getGraph();
  }
  
  /**
   * Condenses the package graph to an acyclic graph of its strong components.
   * @return the duration of this operation in milliseconds.
   * @throws IllegalStateException if this method is called before
   *         {@link #createPackageGraph()}.
   */
  public long condensePackageGraph() 
  {
    checkPackageGraph("condensePackageGraph()");
    long time = System.currentTimeMillis();
    _packageAnalyser.getCondensedGraph();
    return System.currentTimeMillis() - time;
  }

  /**
   * Returns the condensed package graph, i.e.&nbsp;the acyclic graph of 
   * its strong components.
   * @return the duration of this operation in milliseconds.
   * @throws IllegalStateException if this method is called before
   *         {@link #createPackageGraph()}.
   */
  public StrongComponent[] getCondensedPackageGraph() 
  {
    checkPackageGraph("getCondensedPackageGraph()");
    return _packageAnalyser.getCondensedGraph();
  }

  /**
   * Calculates the for each package its layer index. The layer index of a
   * package is the length of the longest path in the acyclic graph of strong 
   * components starting at the strong component to which the package belongs. 
   * @return the duration of this operation in milliseconds.
   * @throws IllegalStateException if this method is called before
   *         {@link #createPackageGraph()}.
   */
  public long calculatePackageLayerMap()
  {
    checkPackageGraph("calculatePackageLayerMap()");
    long time = System.currentTimeMillis();
    _packageAnalyser.getLayerMap();
    return System.currentTimeMillis() - time;
  }

  /**
   * Calculates the for each package its layer index and returns
   * a <tt>Map</tt> where the packages are the keys (type {@link AtomicVertex})
   * and the layer indices are the values (type <tt>Integer</tt>).
   * @throws IllegalStateException if this method is called before
   *         {@link #createPackageGraph()}.
   */
  public Map getPackageLayerMap()
  {
    checkPackageGraph("getPackageLayerMap()");
    return _packageAnalyser.getLayerMap();
  }

  /**
   * Prints an XML report into the specified writer.
   * @param title Title of the report.
   * @param packagesOnly if <tt>true</tt> classes are omitted.
   * @param writer Output stream.
   * @throws IllegalStateException if this method is called before
   *         {@link #createPackageGraph()}.
   */
  public void printXML(String title, boolean packagesOnly, PrintWriter writer) 
  {
    checkPackageGraph("printXML()");
    writer.println("<?xml version='1.0' encoding='UTF-8'?>");
    writer.println("<?xml-stylesheet type='text/xsl' "
                   + "href='reportXMLtoHTML.xsl'?>");
    writer.println("<classycle title='" + Text.excapeForXML(title) + "'>");
    if (!packagesOnly)
    {
      StrongComponent[] components = getCondensedClassGraph();
      writer.println("  <cycles>");
      StrongComponentRenderer sRenderer
          = new XMLStrongComponentRenderer(2);
      for (int i = 0; i < components.length; i++) 
      {
        writer.print(sRenderer.render(components[i]));
      }
      writer.println("  </cycles>");
      writer.println("  <classes numberOfExternalClasses=\"" 
                     + getNumberOfExternalClasses() + "\">");
      AtomicVertex[] graph = getClassGraph();
      Map layerMap = getClassLayerMap();
      render(graph, layerMap, new XMLClassRenderer(), writer);
      writer.println("  </classes>");
    }
    StrongComponent[] components = getCondensedPackageGraph();
    writer.println("  <packageCycles>");
    StrongComponentRenderer sRenderer
          = new XMLPackageStrongComponentRenderer(2);
    for (int i = 0; i < components.length; i++) 
    {
      writer.print(sRenderer.render(components[i]));
    }
    writer.println("  </packageCycles>");
    writer.println("  <packages>");
    AtomicVertex[] graph = getPackageGraph();
    Map layerMap = getPackageLayerMap();
    render(graph, layerMap, new XMLPackageRenderer(), writer);
    writer.println("  </packages>");
    
    writer.println("</classycle>");
    writer.close();
  }

  private void checkPackageGraph(String method)
  {
    if (_packageAnalyser == null)
    {
      throw new IllegalStateException(
                  method + " should be invoked after createPackageGraph().");
    }
  }

  /**
   * Main method of the Analyser. Prints on the console its usage if
   * some invalid command line argument occurs or is missed.
   * @param args command line arguments.
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    AnalyserCommandLine commandLine = new AnalyserCommandLine(args);
    if (!commandLine.isValid()) {
      System.out.println("Usage: java -jar classycle.jar " 
                         + commandLine.getUsage());
      System.exit(0);
    }
    
    Analyser analyser = new Analyser(commandLine.getClassFiles(), 
                                     commandLine.getPattern());
    readAndAnalyse(commandLine, analyser);

    // Create report(s)
    if (commandLine.getXmlFile() != null) 
    {
      analyser.printXML(commandLine.getTitle(), commandLine.isPackagesOnly(),
                    new PrintWriter(new FileWriter(commandLine.getXmlFile())));
    }
    if (commandLine.getCsvFile() != null) 
    {
      analyser.printCSV(
                  new PrintWriter(new FileWriter(commandLine.getCsvFile())));
    }
    if (commandLine.isRaw()) 
    {
      analyser.printRaw(new PrintWriter(System.out));
    }
    if (commandLine.isCycles() || commandLine.isStrong()) 
    {
      analyser.printComponents(new PrintWriter(System.out), 
                               commandLine.isCycles() ? 2 : 1);
    }
  }

  private static void readAndAnalyse(AnalyserCommandLine commandLine,
                                     Analyser analyser) throws IOException
  {
    System.out.println("============= Classycle V" + VERSION 
                       + " =============");
    System.out.println("========== by Franz-Josef Elmer ==========");
    System.out.print("read class files and create class graph ... ");
    long duration = analyser.createClassGraph();
    System.out.println("done after " + duration + " ms: " 
                       + analyser.getClassGraph().length + " classes analysed.");
    
    if (!commandLine.isPackagesOnly())
    {
      // Condense class graph
      System.out.print("condense class graph ... ");
      duration = analyser.condenseClassGraph();
      System.out.println("done after " + duration + " ms: " 
                         + analyser.getCondensedClassGraph().length 
                         + " strong components found.");
    
      // Calculate class layer 
      System.out.print("calculate class layer indices ... ");
      duration = analyser.calculateClassLayerMap();
      System.out.println("done after " + duration + " ms.");
    }
    System.out.print("create package graph ... ");
    duration = analyser.createPackageGraph();
    System.out.println("done after " + duration + " ms: " 
                       + analyser.getPackageGraph().length + " packages.");
    // Condense package graph
    System.out.print("condense package graph ... ");
    duration = analyser.condensePackageGraph();
    System.out.println("done after " + duration + " ms: " 
                       + analyser.getCondensedPackageGraph().length 
                       + " strong components found.");
    // Calculate package layer 
    System.out.print("calculate package layer indices ... ");
    duration = analyser.calculatePackageLayerMap();
    System.out.println("done after " + duration + " ms.");
  }

  
  private static void render(AtomicVertex[] graph, Map layerMap,
                             AtomicVertexRenderer renderer, 
                             PrintWriter writer)  
 {
    for (int i = 0; i < graph.length; i++) 
    {
      Integer layerIndex = (Integer) layerMap.get(graph[i]);
      writer.print(renderer.render(graph[i], 
          layerIndex == null ? -1 : layerIndex.intValue()));
    }
  }

} //class
