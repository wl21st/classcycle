/*
 * Copyright (c) 2003, Franz-Josef Elmer, All rights reserved.
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
package classycle.renderer;

import java.text.MessageFormat;

import classycle.ClassAttributes;
import classycle.graph.GraphAttributes;
import classycle.graph.StrongComponent;
import classycle.graph.Vertex;

public class XMLStrongComponentRenderer extends AbstractStrongComponentRenderer
{
  public static final String CYCLE_ELEMENT = "cycle",
                             CLASS_REF_ELEMENT = "classRef",
                             CLASSES_ELEMENT = "classes",
                             CENTER_CLASSES_ELEMENT = "centerClasses";
  private static final MessageFormat CYCLES_START_FORMAT
      = new MessageFormat("    <" + CYCLE_ELEMENT
                      + " name=\"{0}\" size=\"{1}\" longestWalk=\"{2}\""
                      + " girth=\"{3}\" radius=\"{4}\" diameter=\"{5}\">\n");
  private static final String CYCLES_END_TEMPLATE
      = "    </" + CYCLE_ELEMENT + ">\n";
  private static final String CLASSES_START_TEMPLATE
      = "      <" + CLASSES_ELEMENT + ">\n";
  private static final String CLASSES_END_TEMPLATE
      = "      </" + CLASSES_ELEMENT + ">\n";
  private static final String CENTER_CLASSES_START_TEMPLATE
      = "      <" + CENTER_CLASSES_ELEMENT + ">\n";
  private static final String CENTER_CLASSES_END_TEMPLATE
      = "      </" + CENTER_CLASSES_ELEMENT + ">\n";
  private static final MessageFormat CLASS_REF_FORMAT
      = new MessageFormat("        <" + CLASS_REF_ELEMENT
                          + " name=\"{0}\"/>\n");

  private final int _minimumSize;

  public XMLStrongComponentRenderer(int minimumSize)
  {
    _minimumSize = minimumSize;
  }

  public String render(StrongComponent component)
  {
    StringBuffer result = new StringBuffer();
    if (component.getNumberOfVertices() >= _minimumSize)
    {
      String[] values = new String[6];
      values[0] = createName(component);
      values[1] = Integer.toString(component.getNumberOfVertices());
      values[2] = Integer.toString(component.getLongestWalk());
      GraphAttributes attributes = (GraphAttributes) component.getAttributes();
      values[3] = Integer.toString(attributes.getGirth());
      values[4] = Integer.toString(attributes.getRadius());
      values[5] = Integer.toString(attributes.getDiameter());
      CYCLES_START_FORMAT.format(values, result, null);

      result.append(CLASSES_START_TEMPLATE);
      for (int i = 0, n = component.getNumberOfVertices(); i < n; i++)
      {
        values[0] = ((ClassAttributes) component.getVertex(i).getAttributes())
            .getName();
        CLASS_REF_FORMAT.format(values, result, null);
      }
      result.append(CLASSES_END_TEMPLATE);

      result.append(CENTER_CLASSES_START_TEMPLATE);
      Vertex[] centerVertices = attributes.getCenterVertices();
      for (int i = 0; i < centerVertices.length; i++)
      {
        values[0] = ((ClassAttributes) centerVertices[i].getAttributes())
            .getName();
        CLASS_REF_FORMAT.format(values, result, null);
      }
      result.append(CENTER_CLASSES_END_TEMPLATE);

      result.append(CYCLES_END_TEMPLATE);
    }
    return new String(result);
  }
} //class