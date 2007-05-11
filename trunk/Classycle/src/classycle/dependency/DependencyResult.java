/*
 * Copyright (c) 2003-2006, Franz-Josef Elmer, All rights reserved.
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
package classycle.dependency;

import classycle.graph.AtomicVertex;
import classycle.util.StringPattern;

/**
 * @author  Franz-Josef Elmer
 */
public class DependencyResult implements Result
{
  static final String OK = "\tOK";
  static final String DEPENDENCY_FOUND = "\n  Dependency found:";
  private final StringPattern _startSet;
  private final StringPattern _finalSet;
  private final String _statement;
  private final AtomicVertex[] _paths;
  private final boolean _ok;
  
  public DependencyResult(StringPattern startSet, StringPattern finalSet,
                          String statement, AtomicVertex[] paths)
  {
    _startSet = startSet;
    _finalSet = finalSet;
    _statement = statement;
    _paths = paths;
    _ok = paths.length == 0;
  }

  public boolean isOk()
  {
    return _ok;
  }

  public StringPattern getFinalSet()
  {
    return _finalSet;
  }

  public AtomicVertex[] getPaths()
  {
    return _paths;
  }

  public StringPattern getStartSet()
  {
    return _startSet;
  }

  public String toString()
  {
    StringBuffer buffer = new StringBuffer(_statement);
    if (_ok)
    {
      buffer.append(OK);
    } else
    {
      DependencyPathsRenderer renderer 
              = new DependencyPathsRenderer(_paths, 
                                         new PatternVertexCondition(_startSet), 
                                         new PatternVertexCondition(_finalSet));
      buffer.append(DEPENDENCY_FOUND).append(renderer.renderGraph("  "));
    }
    return new String(buffer.append('\n'));
  }
}