// data

val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ+-*/=$"

case class Elimination(
  label: String,
  participants: Seq[Seq[Player]],
  double: Boolean,
  rounds: Int,
  verticalSteps: Int,
  drops: Seq[Seq[Player]],
  extras: Seq[SvgContext => xml.Elem],
  page: Int = 1,
) {
  val numOfParticipants = participants.flatten.length
  val multipageDrops = drops.nonEmpty && numOfParticipants > 16
  val isPowerOfTwo = numOfParticipants.toBinaryString.toList match {
    case h :: t if h == '1' => t.forall(_ == '0')
    case _ => false
  }
  val firstRoundOfDrops = if (isPowerOfTwo) 2 else 3
}
case class Player(label: Option[String], round: Int, topOffset: Int)
case class Match(label: Option[String], p1: Player, p2: Player, shiftUp: Option[Int] = None) {
  val winner: Player = Player(
    None,
    math.max(p1.round, p2.round) + 1,
    (p1.topOffset + p2.topOffset) / 2 - shiftUp.getOrElse(0)
  )
}

case class PlayOffSettings(print: (Int => Boolean), page: Int, shift: (Int => Int)) {
  def getLabel(players: Int)(round: Int, game: Int): Option[String] = {
    val alphabetForRound: String = alphabet.drop((page - 1) * players / 2)

    if (print(round)) Some(round.toString + alphabetForRound(game))
    else None
  }
}
def noLabelSettings(page: Int) = PlayOffSettings(_ => false, page, _ => 0)
def labelAllSettings(page: Int) = PlayOffSettings(_ => true, page, _ => 0)


// functions

def getPower(p: Int): Int =
  if (p > 2) 1 + getPower(p / 2)
  else 1

def toPlayers(numbers: Seq[Int]): Seq[Seq[Player]] =
  Seq(numbers.zipWithIndex.map {
    case (label, i) => Player(Some(label.toString), 1, 2+2*i)
  })

def seedNumbers(players: Seq[Int], step: Int, steps: Int): Seq[Int] =
  if (step > steps) players
  else {
    val magic = scala.math.pow(2, step).toInt + 1
    val newPlayers = players.flatMap(p => Seq(p, magic - p))
    seedNumbers(newPlayers, step + 1, steps)
  }
def seedNumbers(participants: Int): Seq[Int] =
  seedNumbers(Seq(1), 1, getPower(participants))

def playersMatch(
  players: Seq[Player],
  round: Int,
  labelFromRoundAndGame: (Int, Int) => Option[String],
  shiftUp: Option[Int] = None,
): Seq[Match] = {
  players.sortBy(_.topOffset).sliding(2, 2).zipWithIndex.flatMap {
    case (List(p1, p2), i) => {
      val label = labelFromRoundAndGame(round, i)
      Some(Match(label, p1, p2, shiftUp))
    }
    case _ => None
  }.toSeq
}

def playOff(
  round: Int,
  players: Seq[Seq[Player]],
  drops: Seq[Seq[Player]],
  playOffSettings: PlayOffSettings,
): (Seq[Match], Seq[Seq[Player]]) = {
  val thisRoundPlayers = players.headOption.getOrElse(Nil)
  val allPlayers = thisRoundPlayers ++ drops.headOption.getOrElse(Nil)
  val matches = playersMatch(
    allPlayers,
    round,
    playOffSettings.getLabel(thisRoundPlayers.length),
    Some(playOffSettings.shift(round)),
  )
  val winners: Seq[Player] = matches.map(_.winner)

  if (winners.length <= 1 && drops.flatten.length < 1) {
    // there's no more than one winner and there are no more drops
    if (winners.length == 0) (matches, Seq(allPlayers))
    else (matches, Seq(winners))
  } else {
    val nextRoundPlayers = (players.drop(1).headOption.getOrElse(Nil) ++ winners) +: players.drop(2)
    val (moreMatches, moreWinners) =
      playOff(round + 1, nextRoundPlayers, drops.drop(1), playOffSettings)
    (moreMatches ++ matches, moreWinners.prepended(winners))
  }
}



// view

def loserLabelBottomRight(context: SvgContext): xml.Elem = {
  val (x, y) = (2350, 1300)
  <text y={s"$y"} style={context.bigStyle}>
    <tspan xml:space="preserve" x={s"$x"} dy="1.2em">drabinka</tspan>
    <tspan xml:space="preserve" x={s"$x"} dy="1.2em"> drugiej</tspan>
    <tspan xml:space="preserve" x={s"$x"} dy="1.2em"> nadziei</tspan>
  </text>
}

def loserLabelBottomLeft(context: SvgContext): xml.Elem = {
  val (x, y) = (100, 1500)
  <text x={s"$x"} y={s"$y"}
      transform={s"rotate(-90,$x,$y)"} style={context.bigStyle}>
    <tspan xml:space="preserve" x="0" dy="1.2em">drabinka</tspan>
    <tspan xml:space="preserve" x="0" dy="1.2em"> drugiej</tspan>
    <tspan xml:space="preserve" x="0" dy="1.2em"> nadziei</tspan>
  </text>
}

def placeMedal(place: Int, round: Int, topOffset: Int)(context: SvgContext): xml.Elem = {
  val (x, y) = context.getPlayerCoords(Player(None, round, topOffset))
  <g transform={s"translate(${x + context.playerWidth/2 - 25},${y})"}>
    <text text-anchor="middle" x="25" y="45" style={context.bigStyle}>{place.toString}</text>
    <circle r="35" cx="25" cy="25" style={context.pathStyle} />
    <path style={context.pathStyle} d="m  2,-4 l -15,-20 h  28 l  7,13" />
    <path style={context.pathStyle} d="m 48,-4 l  15,-20 h -28 l -7,13" />
  </g>
}

def dummyMatch(round: Int, from: Int, to: Int)(context: SvgContext): xml.Elem =
  context.getMatchPath(Match(
    None,
    Player(None, round, from),
    Player(None, round, to),
  ))

def dummyPlayer(label: Option[String], round: Int, topOffset: Int)(context: SvgContext): xml.Elem =
  context.getPlayerPath(Player(label, round, topOffset))

case class SvgContext(height: Int, width: Int, padding: Int, e: Elimination) {
  val contentHeight = height - padding * 2
  val contentWidth = width - padding * 2 - 40
  val playerWidth = contentWidth / (e.rounds + 1)
  val fontStyle = "font-size: 40; font-family: Fira Mono; font-variant-numeric: lining-nums tabular-nums;"
  val bigStyle = "font-size: 60; font-family: Fira Mono;"
  val pathStyle = "stroke: black; stroke-width: 4; stroke-linecap: round; fill: none;"

  def getPlayerCoords(p: Player): (Int, Int) = {
    val x = 40 + padding + ((p.round - 1) * contentWidth) / (e.rounds + 1)
    val y = -40 + padding + (p.topOffset * contentHeight) / e.verticalSteps
    (x, y)
  }

  def getPlayerPath(p: Player): xml.Elem = {
    val (x, y) = getPlayerCoords(p)
    val text = p.label match {
      case Some(label) => <text x={(x - 4).toString} y={y.toString}
            text-anchor="end"
            style={fontStyle}>{label}</text>
      case None => xml.Text("")
    }

    <g>
      {text}
      <path d={s"M $x,$y h $playerWidth"} style={pathStyle}/>
    </g>
  }

  def getMatchPath(m: Match): xml.Elem = {
    val (x1, y1) = getPlayerCoords(m.p1)
    val (x2, y2) = getPlayerCoords(m.p2)
    val (mx, my) = getPlayerCoords(m.winner)
    val text = m.label match {
      case Some(label) => <text x={(mx - 10).toString} y={my.toString}
              dy=".35em" text-anchor="end" style={fontStyle}>{label}</text>
      case _ => xml.Text("")
    }
    val extend =
      if (x2 > x1) {
        <path d={s"M ${x1+playerWidth},${y1} h ${x2 - x1}"} style={pathStyle}/>
      } else xml.Text("")

    <g>
      {extend} {text}
      <path d={s"M ${x2+playerWidth},${y1} L ${x2+playerWidth},${y2}"} style={pathStyle}/>
    </g>
  }

  def getRoundPath(r: Int): xml.Elem = {
    val roundModifier =
      if (e.multipageDrops) e.firstRoundOfDrops - 1
      else 0

    val text = if (r == 1) "runda" else {s"${r + roundModifier}."}
    val (x, y) = getPlayerCoords(Player(None, r, e.verticalSteps))
    <text x={(x + playerWidth/2).toString} y={(y + 40).toString}
      text-anchor="middle" style={bigStyle}>{text}</text>
  }
}


def draw(e: Elimination): xml.Elem = {
  val initialPlayers: Seq[Seq[Player]] = e.participants

  def getDoublePlayersAndMatches() = {
    val (losermatches, losers) = playOff(e.firstRoundOfDrops, Nil, e.drops, noLabelSettings(e.page))

    if (e.multipageDrops) {
      // loser bracket only
      (e.drops.flatten ++ losers.flatten,
      losermatches)
    } else {
      // winner & loser bracket (loser bracket empty if no drops)
      val (winnermatches, winners) = playOff(1, initialPlayers, Nil, labelAllSettings(e.page))

      val finale: Seq[Match] = playersMatch(winners.last ++ losers.last, e.rounds, (_, _) => None)
      val winner: Seq[Player] = finale.map(_.winner)

      (initialPlayers.flatten ++ winners.flatten ++ e.drops.flatten ++ losers.flatten ++ winner,
      winnermatches ++ losermatches ++ finale)
    }
  }

  def shiftRound(round: Int): Int =
    // shift rounds up from second round until semifinal
    if (round + 1 < e.rounds) round - 1
    else 0

  def getSinglePlayersAndMatches() = {
    val singlePlayerSettings = PlayOffSettings(_ + 1 == e.rounds, e.page, shiftRound)
    val (winnermatches, winners) = playOff(1, initialPlayers, Nil, singlePlayerSettings)
    (initialPlayers.flatten ++ winners.flatten,
    winnermatches)
  }

  val (players: Seq[Player], matches: Seq[Match]) =
    if (e.double) getDoublePlayersAndMatches()
    else getSinglePlayersAndMatches()

  val context = SvgContext(2100, 2970, 50, e)

  <svg
      xmlns="http://www.w3.org/2000/svg"
      xmlns:xlink="http://www.w3.org/1999/xlink"
      viewBox={s"0 0 ${context.width} ${context.height}"}
      width="297mm" height="210mm">
    <rect fill="white" x="0" y="0"
      width={context.width.toString} height={context.height.toString} />
    { players.map(context.getPlayerPath) }
    { matches.map(context.getMatchPath) }
    { (1 to e.rounds).map(context.getRoundPath) }
    { e.extras.map(extra => extra(context)) }
  </svg>
}


// assemble the ladders

val seed24 = Seq(
  Seq(
    Player(Some("16"), 1,  6),
    Player(Some("17"), 1, 10),
    Player(Some( "9"), 1, 16),
    Player(Some("24"), 1, 20),
    Player(Some("13"), 1, 26),
    Player(Some("20"), 1, 30),
    Player(Some("12"), 1, 36),
    Player(Some("21"), 1, 40),
    Player(Some("15"), 1, 46),
    Player(Some("18"), 1, 50),
    Player(Some("10"), 1, 56),
    Player(Some("23"), 1, 60),
    Player(Some("14"), 1, 66),
    Player(Some("19"), 1, 70),
    Player(Some("11"), 1, 76),
    Player(Some("22"), 1, 80),
  ),
  Seq(
    Player(Some( "1"), 2,  4),
    Player(Some( "8"), 2, 14),
    Player(Some( "4"), 2, 24),
    Player(Some( "5"), 2, 34),
    Player(Some( "2"), 2, 44),
    Player(Some( "7"), 2, 54),
    Player(Some( "3"), 2, 64),
    Player(Some( "6"), 2, 74),
  ),
)

val seed48a = Seq(
  Seq(
    Player(Some("32"), 1,  6),
    Player(Some("33"), 1, 10),
    Player(Some("17"), 1, 16),
    Player(Some("48"), 1, 20),
    Player(Some("25"), 1, 26),
    Player(Some("40"), 1, 30),
    Player(Some("24"), 1, 36),
    Player(Some("41"), 1, 40),
    Player(Some("29"), 1, 46),
    Player(Some("36"), 1, 50),
    Player(Some("20"), 1, 56),
    Player(Some("45"), 1, 60),
    Player(Some("28"), 1, 66),
    Player(Some("37"), 1, 70),
    Player(Some("21"), 1, 76),
    Player(Some("44"), 1, 80),
  ),
  Seq(
    Player(Some( "1"), 2,  4),
    Player(Some("16"), 2, 14),
    Player(Some( "8"), 2, 24),
    Player(Some( "9"), 2, 34),
    Player(Some( "4"), 2, 44),
    Player(Some("13"), 2, 54),
    Player(Some( "5"), 2, 64),
    Player(Some("12"), 2, 74),
  ),
)

val seed48b = Seq(
  Seq(
    Player(Some("31"), 1,  6),
    Player(Some("34"), 1, 10),
    Player(Some("18"), 1, 16),
    Player(Some("47"), 1, 20),
    Player(Some("26"), 1, 26),
    Player(Some("39"), 1, 30),
    Player(Some("23"), 1, 36),
    Player(Some("42"), 1, 40),
    Player(Some("30"), 1, 46),
    Player(Some("35"), 1, 50),
    Player(Some("19"), 1, 56),
    Player(Some("46"), 1, 60),
    Player(Some("27"), 1, 66),
    Player(Some("38"), 1, 70),
    Player(Some("22"), 1, 76),
    Player(Some("43"), 1, 80),
  ),
  Seq(
    Player(Some( "2"), 2,  4),
    Player(Some("15"), 2, 14),
    Player(Some( "7"), 2, 24),
    Player(Some("10"), 2, 34),
    Player(Some( "3"), 2, 44),
    Player(Some("14"), 2, 54),
    Player(Some( "6"), 2, 64),
    Player(Some("11"), 2, 74),
  ),
)


val drops8 = Seq(
  Seq(
    Player(Some("1A"), 2, 20),
    Player(Some("1B"), 2, 22),
    Player(Some("1C"), 2, 26),
    Player(Some("1D"), 2, 28),
  ),
  Seq(
    Player(Some("2B"), 3, 19),
    Player(Some("2A"), 3, 25),
  ),
  Seq(),
  Seq(
    Player(Some("3A"), 5, 18),
  ),
)

val drops16 = Seq(
  Seq(
    Player(Some("1A"), 2, 34),
    Player(Some("1B"), 2, 36),
    Player(Some("1E"), 2, 39),
    Player(Some("1F"), 2, 41),
    Player(Some("1C"), 2, 44),
    Player(Some("1D"), 2, 46),
    Player(Some("1G"), 2, 49),
    Player(Some("1H"), 2, 51),
  ),
  Seq(
    Player(Some("2B"), 3, 33),
    Player(Some("2D"), 3, 38),
    Player(Some("2A"), 3, 43),
    Player(Some("2C"), 3, 48),
  ),
  Seq(
    Player(Some("3B"), 4, 32),
    Player(Some("3A"), 4, 37),
  ),
  Seq(
    Player(Some("4A"), 5, 42),
  ),
)

val drops24 = Seq(
  Seq(
    Player(Some("1A"), 1,  3),
    Player(Some("2C"), 1,  5),
    Player(Some("1B"), 1,  7),
    Player(Some("2D"), 1,  9),
    Player(Some("1E"), 1, 13),
    Player(Some("2G"), 1, 15),
    Player(Some("1F"), 1, 17),
    Player(Some("2H"), 1, 19),
    Player(Some("1C"), 1, 23),
    Player(Some("2E"), 1, 25),
    Player(Some("1D"), 1, 27),
    Player(Some("2F"), 1, 29),
    Player(Some("1G"), 1, 33),
    Player(Some("2A"), 1, 35),
    Player(Some("1H"), 1, 37),
    Player(Some("2B"), 1, 39),
  ),
  Seq(),
  Seq(
    Player(Some("3C"), 3,  2),
    Player(Some("3A"), 3, 12),
    Player(Some("3D"), 3, 22),
    Player(Some("3B"), 3, 32),
  ),
  Seq(
    Player(Some("4A"), 4,  9),
    Player(Some("4B"), 4, 19),
  ),
  Seq(
    Player(Some("5A"), 5, 37),
  ),
)

val drops32 = Seq(
  Seq(
    Player(Some("1A"), 1,  3),
    Player(Some("1B"), 1,  5),
    Player(Some("1C"), 1,  8),
    Player(Some("1D"), 1, 10),
    Player(Some("1I"), 1, 13),
    Player(Some("1J"), 1, 15),
    Player(Some("1K"), 1, 18),
    Player(Some("1L"), 1, 20),
    Player(Some("1E"), 1, 23),
    Player(Some("1F"), 1, 25),
    Player(Some("1G"), 1, 28),
    Player(Some("1H"), 1, 30),
    Player(Some("1M"), 1, 33),
    Player(Some("1N"), 1, 35),
    Player(Some("1O"), 1, 38),
    Player(Some("1P"), 1, 40),
  ),
  Seq(
    Player(Some("2C"), 2,  2),
    Player(Some("2D"), 2,  7),
    Player(Some("2G"), 2, 12),
    Player(Some("2H"), 2, 17),
    Player(Some("2E"), 2, 22),
    Player(Some("2F"), 2, 27),
    Player(Some("2A"), 2, 32),
    Player(Some("2B"), 2, 37),
  ),
  Seq(),
  Seq(
    Player(Some("3C"), 4,  2),
    Player(Some("3A"), 4, 12),
    Player(Some("3D"), 4, 22),
    Player(Some("3B"), 4, 32),
  ),
  Seq(
    Player(Some("4A"), 5,  8),
    Player(Some("4B"), 5, 18),
  ),
  Seq(
    Player(Some("5A"), 6, 36),
  ),
)

val drops48 = Seq(
  Seq(
    Player(Some("1A"), 1,  2),
    Player(Some("2E"), 1,  4),
    Player(Some("1B"), 1,  7),
    Player(Some("2F"), 1,  9),
    Player(Some("1C"), 1, 12),
    Player(Some("2G"), 1, 14),
    Player(Some("1D"), 1, 16),
    Player(Some("2H"), 1, 18),
    Player(Some("1I"), 1, 20),
    Player(Some("2M"), 1, 22),
    Player(Some("1J"), 1, 25),
    Player(Some("2N"), 1, 27),
    Player(Some("1K"), 1, 30),
    Player(Some("2O"), 1, 32),
    Player(Some("1L"), 1, 34),
    Player(Some("2P"), 1, 36),
    Player(Some("1E"), 1, 38),
    Player(Some("2I"), 1, 40),
    Player(Some("1F"), 1, 43),
    Player(Some("2J"), 1, 45),
    Player(Some("1G"), 1, 48),
    Player(Some("2K"), 1, 50),
    Player(Some("1H"), 1, 52),
    Player(Some("2L"), 1, 54),
    Player(Some("1M"), 1, 56),
    Player(Some("2A"), 1, 58),
    Player(Some("1N"), 1, 61),
    Player(Some("2B"), 1, 63),
    Player(Some("1O"), 1, 66),
    Player(Some("2C"), 1, 68),
    Player(Some("1P"), 1, 70),
    Player(Some("2D"), 1, 72),
  ),
  Seq(
    Player(Some("3E"), 2,  6),
    Player(Some("3F"), 2, 11),
    Player(Some("3A"), 2, 24),
    Player(Some("3B"), 2, 29),
    Player(Some("3G"), 2, 42),
    Player(Some("3H"), 2, 47),
    Player(Some("3C"), 2, 60),
    Player(Some("3D"), 2, 65),
  ),
  Seq(
    Player(Some("4D"), 3, 19),
    Player(Some("4B"), 3, 37),
    Player(Some("4A"), 3, 55),
    Player(Some("4C"), 3, 73),
  ),
  Seq(
  ),
  Seq(
    Player(Some("5A"), 5, 20),
    Player(Some("5B"), 5, 38),
  ),
  Seq(
    Player(Some("6A"), 6, 70),
  ),
)

val drops64 = Seq(
  Seq(
    Player(Some("1A"), 1,  3),
    Player(Some("1B"), 1,  5),
    Player(Some("1C"), 1,  8),
    Player(Some("1D"), 1, 10),
    Player(Some("1E"), 1, 13),
    Player(Some("1F"), 1, 15),
    Player(Some("1G"), 1, 18),
    Player(Some("1H"), 1, 20),
    Player(Some("1Q"), 1, 23),
    Player(Some("1R"), 1, 25),
    Player(Some("1S"), 1, 28),
    Player(Some("1T"), 1, 30),
    Player(Some("1U"), 1, 33),
    Player(Some("1V"), 1, 35),
    Player(Some("1W"), 1, 38),
    Player(Some("1X"), 1, 40),
    Player(Some("1I"), 1, 43),
    Player(Some("1J"), 1, 45),
    Player(Some("1K"), 1, 48),
    Player(Some("1L"), 1, 50),
    Player(Some("1M"), 1, 53),
    Player(Some("1N"), 1, 55),
    Player(Some("1O"), 1, 58),
    Player(Some("1P"), 1, 60),
    Player(Some("1Y"), 1, 63),
    Player(Some("1Z"), 1, 65),
    Player(Some("1+"), 1, 68),
    Player(Some("1-"), 1, 70),
    Player(Some("1*"), 1, 73),
    Player(Some("1/"), 1, 75),
    Player(Some("1="), 1, 78),
    Player(Some("1$"), 1, 80),
  ),
  Seq(
    Player(Some("2E"), 2,  2),
    Player(Some("2F"), 2,  7),
    Player(Some("2G"), 2, 12),
    Player(Some("2H"), 2, 17),
    Player(Some("2M"), 2, 22),
    Player(Some("2N"), 2, 27),
    Player(Some("2O"), 2, 32),
    Player(Some("2P"), 2, 37),
    Player(Some("2I"), 2, 42),
    Player(Some("2J"), 2, 47),
    Player(Some("2K"), 2, 52),
    Player(Some("2L"), 2, 57),
    Player(Some("2A"), 2, 62),
    Player(Some("2B"), 2, 67),
    Player(Some("2C"), 2, 72),
    Player(Some("2D"), 2, 77),
  ),
  Seq(
    Player(Some("3E"), 3,  6),
    Player(Some("3F"), 3, 11),
    Player(Some("3A"), 3, 26),
    Player(Some("3B"), 3, 31),
    Player(Some("3G"), 3, 46),
    Player(Some("3H"), 3, 51),
    Player(Some("3C"), 3, 66),
    Player(Some("3D"), 3, 71),
  ),
  Seq(
    Player(Some("4D"), 4, 20),
    Player(Some("4B"), 4, 40),
    Player(Some("4A"), 4, 60),
    Player(Some("4C"), 4, 80),
  ),
  Seq(
  ),
  Seq(
    Player(Some("5A"), 6, 20),
    Player(Some("5B"), 6, 40),
  ),
  Seq(
    Player(Some("6A"), 7, 76),
  ),
)

// eliminations

Seq(
  Elimination("single-elim-08.svg", toPlayers(seedNumbers( 8)), false, 3, 21, Nil, Seq(
    dummyPlayer(Some("2A"), 3, 17),
    dummyPlayer(Some("2B"), 3, 19),
    dummyMatch(3, 17, 19),
    dummyPlayer(None, 4, 18),
    placeMedal(1, 4, 10),
    placeMedal(2, 3, 10),
    placeMedal(3, 4, 19),
  )),
  Elimination("single-elim-16.svg", toPlayers(seedNumbers(16)), false, 4, 36, Nil, Seq(
    dummyPlayer(Some("3A"), 4, 31),
    dummyPlayer(Some("3B"), 4, 34),
    dummyMatch(4, 31, 34),
    dummyPlayer(None, 5, 32),
    placeMedal(1, 5, 17),
    placeMedal(2, 4, 17),
    placeMedal(3, 5, 33),
  )),
  Elimination("single-elim-24.svg", seed24, false, 5, 84, Nil, Seq(
    dummyPlayer(Some("4A"), 5, 74),
    dummyPlayer(Some("4B"), 5, 80),
    dummyMatch(5, 74, 80),
    dummyPlayer(None, 6, 77),
    placeMedal(1, 6, 40),
    placeMedal(2, 5, 40),
    placeMedal(3, 6, 79),
  )),
  Elimination("single-elim-32.svg", toPlayers(seedNumbers(32)), false, 5, 66, Nil, Seq(
    dummyPlayer(Some("4A"), 5, 58),
    dummyPlayer(Some("4B"), 5, 62),
    dummyMatch(5, 58, 62),
    dummyPlayer(None, 6, 60),
    placeMedal(1, 6, 32),
    placeMedal(2, 5, 32),
    placeMedal(3, 6, 62),
  )),
  Elimination("single-elim-48-1.svg", seed48a, false, 6, 85, Nil, Seq(
    // off to the final
    dummyMatch(6, 35, 80),
    placeMedal(2, 6, 37),
    // final
    dummyPlayer(None, 7, 65),
    placeMedal(1, 7, 67),
  ), 1),
  Elimination("single-elim-48-2.svg", seed48b, false, 6, 85, Nil, Seq(
    // off to the final
    dummyMatch(6, 35, 6),
    placeMedal(2, 6, 37),
    // third place
    dummyPlayer(Some("5A"), 6, 70),
    dummyPlayer(Some("5B"), 6, 76),
    dummyMatch(6, 70, 76),
    dummyPlayer(None, 7, 73),
    placeMedal(3, 7, 75),
  ), 2),
  Elimination("single-elim-64-1.svg", toPlayers(seedNumbers(64).take(32)), false, 6, 66, Nil, Seq(
    // off to the final
    placeMedal(2, 6, 29),
    dummyMatch(6, 27, 64),
    // final
    placeMedal(1, 7, 46),
    dummyPlayer(None, 7, 44),
  ), 1),
  Elimination("single-elim-64-2.svg", toPlayers(seedNumbers(64).drop(32)), false, 6, 66, Nil, Seq(
    // off to the final
    placeMedal(2, 6, 29),
    dummyMatch(6, 27, 2),
    // third place
    dummyPlayer(Some("5A"), 6, 51),
    dummyPlayer(Some("5B"), 6, 57),
    dummyMatch(6, 51, 57),
    dummyPlayer(None, 7, 54),
    placeMedal(3, 7, 56),
  ), 2),


  Elimination("double-elim-08.svg", toPlayers(seedNumbers( 8)),  true, 6,  30, drops8,  Seq(
    loserLabelBottomLeft,
    placeMedal(1, 7, 15),
    placeMedal(2, 6, 15),
    placeMedal(3, 5, 15),
  )),
  Elimination("double-elim-16.svg", toPlayers(seedNumbers(16)),  true, 7,  52, drops16, Seq(
    loserLabelBottomLeft,
    placeMedal(1, 8, 29),
    placeMedal(2, 7, 29),
    placeMedal(3, 6, 29),
  )),

  Elimination("double-elim-24-1.svg", seed24, true, 6, 84, Nil, Seq(
    dummyMatch(6, 41, 80),
    placeMedal(2, 6, 43),
    // winner
    dummyPlayer(None, 7, 66),
    placeMedal(1, 7, 68),
  )),
  Elimination("double-elim-24-2.svg", seed24, true, 7, 42, drops24, Seq(
    dummyMatch(7, 22, 2),
    loserLabelBottomRight,
    placeMedal(2, 7, 23),
    placeMedal(3, 6, 23),
  )),

  Elimination("double-elim-32-1.svg", toPlayers(seedNumbers(32)), true, 6, 66, Nil, Seq(
    dummyMatch(6, 33, 64),
    placeMedal(2, 6, 35),
    // winner
    dummyPlayer(None, 7, 55),
    placeMedal(1, 7, 57),
  )),
  Elimination("double-elim-32-2.svg", toPlayers(seedNumbers(32)), true, 8, 42, drops32, Seq(
    dummyMatch(8, 21, 2),
    loserLabelBottomRight,
    placeMedal(2, 8, 22),
    placeMedal(3, 7, 22),
  )),

  Elimination("double-elim-48-1.svg", seed48a, true, 7, 85, Nil, Seq(
    // off to the winner's bracket final
    dummyMatch(6, 41, 80),
  ), 1),
  Elimination("double-elim-48-2.svg", seed48b, true, 7, 85, Nil, Seq(
    // off to the winner's bracket final
    ((context: SvgContext) => context.getMatchPath(Match(
      Some("6A"),
      Player(None, 6, 41),
      Player(None, 6,  6),
    ))),
    // winner bracket final
    dummyPlayer(None, 7, 22),
    placeMedal(2, 7, 24),
    // THE final
    dummyMatch(7, 22, 80),
    dummyPlayer(None, 8, 60),
    placeMedal(1, 8, 62),
  ), 2),
  Elimination("double-elim-48-3.svg", seed48a, true, 8, 74, drops48, Seq(
    dummyMatch(8, 43, 2),
    placeMedal(2, 8, 45),
    placeMedal(3, 7, 45),
  ), 1),

  Elimination("double-elim-64-1.svg", toPlayers(seedNumbers(64).take(32)), true, 7, 66, Nil, Seq(
    // off to the winner's bracket final
    dummyMatch(6, 33, 64),
  ), 1),
  Elimination("double-elim-64-2.svg", toPlayers(seedNumbers(64).drop(32)), true, 7, 66, Nil, Seq(
    // off to the winner's bracket final
    ((context: SvgContext) => context.getMatchPath(Match(
      Some("6A"),
      Player(None, 6, 33),
      Player(None, 6,  2),
    ))),
    // winner bracket final
    dummyPlayer(None, 7, 17),
    placeMedal(2, 7, 19),
    // THE final
    dummyMatch(7, 17, 64),
    dummyPlayer(None, 8, 44),
    placeMedal(1, 8, 46),
  ), 2),
  Elimination("double-elim-64-3.svg", toPlayers(seedNumbers(64)), true, 9, 82, drops64, Seq(
    dummyMatch(9, 46, 2),
    placeMedal(2, 9, 48),
    placeMedal(3, 8, 48),
  )),

).map { e =>
  val pp = new xml.PrettyPrinter(100, 2)
  os.write.over(os.pwd/"drabinki-pdf"/e.label, pp.format(draw(e)))
}
