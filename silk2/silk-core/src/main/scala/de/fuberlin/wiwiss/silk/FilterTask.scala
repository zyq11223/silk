/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk

import config.LinkFilter
import de.fuberlin.wiwiss.silk.util.task.Task
import collection.mutable.{ArrayBuffer, Buffer}
import entity.Link

/**
 * Filters the links according to the link limit.
 */
class FilterTask(links: Seq[Link], filter: LinkFilter) extends Task[Seq[Link]] {
  taskName = "Filtering"

  override def execute(): Seq[Link] = {
    filter.limit match {
      case Some(limit) => {
        val linkBuffer = new ArrayBuffer[Link]()
        updateStatus("Filtering output")

        for ((sourceUri, groupedLinks) <- links.groupBy(_.source)) {
          if(filter.unambiguous==Some(true)) {
            if(groupedLinks.distinct.size==1)
              linkBuffer.append(groupedLinks.head)
          } else {
            val bestLinks = groupedLinks.distinct.sortWith(_.confidence.getOrElse(-1.0) > _.confidence.getOrElse(-1.0)).take(limit)
            linkBuffer.appendAll(bestLinks)
          }
        }

        logger.info("Filtered " + links.size + " links yielding " + linkBuffer.size + " links")

        linkBuffer
      }
      case None => links.distinct
    }
  }
}
