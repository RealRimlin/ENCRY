package encry

import encry.SimpleBlockchain.Height
import encry.transaction.{SimpleBlock, EncryBaseTransaction}
import scorex.core.consensus.History.{HistoryComparisonResult, ProgressInfo}
import scorex.core.{ModifierId, ModifierTypeId}
import scorex.core.consensus.{BlockChain, History, ModifierSemanticValidity}
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.crypto.encode.Base58

import scala.util.{Failure, Success, Try}

class SimpleBlockchain(blockIds: Map[Height, ModifierId] = Map(), blocks: Map[ModifierId, SimpleBlock] = Map())
  extends BlockChain[PublicKey25519Proposition, EncryBaseTransaction, SimpleBlock, SimpleSyncInfo, SimpleBlockchain] {

  import BlockChain.Score

  override def isEmpty: Boolean = blocks.isEmpty

  override def modifierById(blockId: ModifierId): Option[SimpleBlock] =
    blocks.find(_._1.sameElements(blockId)).map(_._2)

  override def append(block: SimpleBlock): Try[(SimpleBlockchain, History.ProgressInfo[SimpleBlock])] = synchronized {
    val blockId = block.id
    val parentId = block.parentId

    if (blockIds.isEmpty || (lastBlock.id sameElements parentId)) {
      val h = height() + 1
      val newChain = SimpleBlockchain(blockIds + (h -> blockId), blocks + (blockId -> block))
      Success(newChain, ProgressInfo(None, Seq(), Some(block), Seq()))
    } else {
      val e = new Exception(s"Last block id is ${Base58.encode(blockIds.last._2)}, expected ${Base58.encode(parentId)}}")
      Failure(e)
    }
  }

  override def openSurfaceIds(): Seq[ModifierId] = Seq(blockIds(height()))

  override def continuationIds(info: SimpleSyncInfo,
                               size: Height): Option[Seq[(ModifierTypeId, ModifierId)]] = {
    val from = info.startingPoints
    require(from.size == 1)
    require(from.head._1 == SimpleBlock.ModifierTypeId)

    val fromId = from.head._2

    blockIds.find(_._2 sameElements fromId).map { case (fromHeight, _) =>
      (fromHeight + 1).to(fromHeight + size)
          .flatMap(blockIds.get)
          .map(id => SimpleBlock.ModifierTypeId -> id)
    }
  }

  override def chainScore(): BigInt = blocks.map(ib => score(ib._2)).sum

  override type NVCT = SimpleBlockchain

  override def score(block: SimpleBlock): Score = BigInt("18446744073709551616") / block.baseTarget

  override def height(): Height = blocks.size

  override def heightOf(blockId: ModifierId): Option[Height] =
    blockIds.find(_._2.sameElements(blockId)).map(_._1)

  override def blockAt(height: Height): Option[SimpleBlock] =
    blockIds.get(height).flatMap(blocks.get)

  override def children(blockId: ModifierId): Seq[SimpleBlock] =
    heightOf(blockId).map(_ + 1).flatMap(blockAt).toSeq

  override def syncInfo(answer: Boolean = false): SimpleSyncInfo =
    SimpleSyncInfo(answer, lastBlock.id, chainScore())

  override def compare(other: SimpleSyncInfo): History.HistoryComparisonResult.Value = {
    val local = syncInfo().score
    val remote = other.score
    if (local < remote) HistoryComparisonResult.Older
    else if (local == remote) HistoryComparisonResult.Equal
    else HistoryComparisonResult.Younger
  }


  override def reportSemanticValidity(modifier: SimpleBlock,
                                      valid: Boolean,
                                      lastApplied: ModifierId): (SimpleBlockchain, ProgressInfo[SimpleBlock]) = ???

  override def isSemanticallyValid(modifierId: ModifierId): ModifierSemanticValidity.Value = ???

}

object SimpleBlockchain {
  type Height = Int
}
